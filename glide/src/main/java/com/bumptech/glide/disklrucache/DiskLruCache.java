/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bumptech.glide.disklrucache;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A cache that uses a bounded amount of space on a filesystem. Each cache
 * entry has a string key and a fixed number of values. Each key must match
 * the regex <strong>[a-z0-9_-]{1,120}</strong>. Values are byte sequences,
 * accessible as streams or files. Each value must be between {@code 0} and
 * {@code Integer.MAX_VALUE} bytes in length.
 * <p>
 * <p>The cache stores its data in a directory on the filesystem. This
 * directory must be exclusive to the cache; the cache may delete or overwrite
 * files from its directory. It is an error for multiple processes to use the
 * same cache directory at the same time.
 * <p>
 * <p>This cache limits the number of bytes that it will store on the
 * filesystem. When the number of stored bytes exceeds the limit, the cache will
 * remove entries in the background until the limit is satisfied. The limit is
 * not strict: the cache may temporarily exceed it while waiting for files to be
 * deleted. The limit does not include filesystem overhead or the cache
 * journal so space-sensitive applications should set a conservative limit.
 * <p>
 * <p>Clients call {@link #edit} to create or update the values of an entry. An
 * entry may have only one editor at one time; if a value is not available to be
 * edited then {@link #edit} will return null.
 * <ul>
 * <li>When an entry is being <strong>created</strong> it is necessary to
 * supply a full set of values; the empty value should be used as a
 * placeholder if necessary.
 * <li>When an entry is being <strong>edited</strong>, it is not necessary
 * to supply data for every value; values default to their previous
 * value.
 * </ul>
 * Every {@link #edit} call must be matched by a call to {@link Editor#commit}
 * or {@link Editor#abort}. Committing is atomic: a read observes the full set
 * of values as they were before or after the commit, but never a mix of values.
 * <p>
 * <p>Clients call {@link #get} to read a snapshot of an entry. The read will
 * observe the value at the time that {@link #get} was called. Updates and
 * removals after the call do not impact ongoing reads.
 * <p>
 * <p>This class is tolerant of some I/O errors. If files are missing from the
 * filesystem, the corresponding entries will be dropped from the cache. If
 * an error occurs while writing a cache value, the edit will fail silently.
 * Callers should handle other problems by catching {@code IOException} and
 * responding appropriately.
 * <p>
 * journal文件说明
 * <p>
 * journal文件内容分为两部分：头文件部分和正文部分
 * <p>
 * 正文部分的每行内容的组成格式为：journal state key + 空格 + cache key + 空格 + state-specific values
 * <p>
 * journal state key：DIRTY、CLEAN、READ、REMOVE
 * <p>
 * state-specific values：只有journal state key为CLEAN的正文行才有该部分
 * <p>
 * 注：正文行可能同时有多行记录的cache key是同一个key
 */
public final class DiskLruCache implements Closeable {
    static final String JOURNAL_FILE = "journal";
    static final String JOURNAL_FILE_TEMP = "journal.tmp";
    static final String JOURNAL_FILE_BACKUP = "journal.bkp";
    static final String MAGIC = "libcore.io.DiskLruCache";
    static final String VERSION_1 = "1";
    static final long ANY_SEQUENCE_NUMBER = -1;
    private static final String CLEAN = "CLEAN";
    private static final String DIRTY = "DIRTY";
    private static final String REMOVE = "REMOVE";
    private static final String READ = "READ";

    /*
     * This cache uses a journal file named "journal". A typical journal file
     * looks like this:
     *     libcore.io.DiskLruCache
     *     1
     *     100
     *     2
     *
     *     CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
     *     DIRTY 335c4c6028171cfddfbaae1a9c313c52
     *     CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
     *     REMOVE 335c4c6028171cfddfbaae1a9c313c52
     *     DIRTY 1ab96a171faeeee38496d8b330771a7a
     *     CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
     *     READ 335c4c6028171cfddfbaae1a9c313c52
     *     READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
     *
     * The first five lines of the journal form its header. They are the
     * constant string "libcore.io.DiskLruCache", the disk cache's version,
     * the application's version, the value count, and a blank line.
     *
     * Each of the subsequent lines in the file is a record of the state of a
     * cache entry. Each line contains space-separated values: a state, a key,
     * and optional state-specific values.
     *   o DIRTY lines track that an entry is actively being created or updated.
     *     Every successful DIRTY action should be followed by a CLEAN or REMOVE
     *     action. DIRTY lines without a matching CLEAN or REMOVE indicate that
     *     temporary files may need to be deleted.
     *   o CLEAN lines track a cache entry that has been successfully published
     *     and may be read. A publish line is followed by the lengths of each of
     *     its values.
     *   o READ lines track accesses for LRU.
     *   o REMOVE lines track entries that have been deleted.
     *
     * The journal file is appended to as cache operations occur. The journal may
     * occasionally be compacted by dropping redundant lines. A temporary file named
     * "journal.tmp" will be used during compaction; that file should be deleted if
     * it exists when the cache is opened.
     */

    private final File directory;
    private final File journalFile;
    /**
     * 调用rebuildJorunal重新创建日志文件的时候会把日志信息写入到该文件中去
     */
    private final File journalFileTmp;
    /**
     * 重构journal文件时，会将journal文件重命名为journal.bkp文件
     */
    private final File journalFileBackup;
    private final int appVersion;
    private long maxSize;
    /**
     * 一张图片文件由几个文件组成，一般一个就可以了
     */
    private final int valueCount;
    /**
     * 用于记录所有图片磁盘缓存文件的总大小
     */
    private long size = 0;
    private Writer journalWriter;
    /**
     * 用于存储非REMOVE型的Entry对象的集合，采用LinkedHashMap是因为要按先后顺序进行存取
     * （即先进先出的队列模式）
     */
    private final LinkedHashMap<String, Entry> lruEntries = new LinkedHashMap<String, Entry>(0, 0.75f, true);
    /**
     * 用来判断是否重建日志的字段
     * <p>
     * 用于记录多余的正文行
     * <p>
     * 若cache key生成了多条不同类型的记录，则其最多是最后一次出现的记录为有效记录
     * 有效记录对应的Entry对象会存储到集合中去
     * <p>
     * 之所以说是最多，是因为若最后一次出现的记录为REMOVE型，那么该cache key在集合中就没有对应的Entry对象值了
     */
    private int redundantOpCount;

    /**
     * To differentiate between old and current snapshots, each entry is given
     * a sequence number each time an edit is committed. A snapshot is stale if
     * its sequence number is not equal to its entry's sequence number.
     */
    private long nextSequenceNumber = 0;

    /**
     * This cache uses a single background thread to evict entries.
     */
    final ThreadPoolExecutor executorService =
            new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                    new DiskLruCacheThreadFactory());
    /**
     * 用来检查文件个数和大小是否符合规范，若不符合，则执行相应操作；检查journal文件是否需要重构，
     * 若需要，则进行重构操作
     */
    private final Callable<Void> cleanupCallable = new Callable<Void>() {
        public Void call() throws Exception {
            synchronized (DiskLruCache.this) {
                if (journalWriter == null) {
                    return null; // Closed.
                }
                trimToSize();
                if (journalRebuildRequired()) {
                    rebuildJournal();
                    redundantOpCount = 0;
                }
            }
            return null;
        }
    };

    private DiskLruCache(File directory, int appVersion, int valueCount, long maxSize) {
        this.directory = directory;
        this.appVersion = appVersion;
        this.journalFile = new File(directory, JOURNAL_FILE);
        this.journalFileTmp = new File(directory, JOURNAL_FILE_TEMP);
        this.journalFileBackup = new File(directory, JOURNAL_FILE_BACKUP);
        this.valueCount = valueCount;
        this.maxSize = maxSize;
    }

    /**
     * Opens the cache in {@code directory}, creating a cache if none exists there.
     * <p>
     * 1.journal.bkp文件存在的情况下，判断journal文件是否存在，若存在，则删除journal.bkp文件，
     * 否则就将journal.bkp文件名命名成journal
     * <p>
     * 2.如果journal文件不存在，则创建journal文件，并填充头文件部分
     * <p>
     * 3.通过journal文件的正文部分的记录，填充存储Entry对象的集合，并只保留集合中的CLEAN型的Entry对象
     *
     * @param directory  a writable directory
     * @param valueCount the number of values per cache entry. Must be positive.
     * @param maxSize    the maximum number of bytes this cache should use to store
     * @throws IOException if reading or writing the cache directory fails
     */
    public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize)
            throws IOException {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        if (valueCount <= 0) {
            throw new IllegalArgumentException("valueCount <= 0");
        }

        // If a bkp file exists, use it instead.
        File backupFile = new File(directory, JOURNAL_FILE_BACKUP);
        /**
         * 若journal.bkp文件存在，则说明上次重构journal文件失败
         * （如在重构文件期间，手机突然断电等特殊情况发生了）
         */
        if (backupFile.exists()) {
            File journalFile = new File(directory, JOURNAL_FILE);
            // If journal file also exists just delete backup file.
            if (journalFile.exists()) {
                backupFile.delete();
            } else {
                renameTo(backupFile, journalFile, false);
            }
        }

        // Prefer to pick up where we left off.
        DiskLruCache cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
        if (cache.journalFile.exists()) {
            try {
                cache.readJournal();
                cache.processJournal();
                return cache;
            } catch (IOException journalIsCorrupt) {
                System.out
                        .println("DiskLruCache "
                                + directory
                                + " is corrupt: "
                                + journalIsCorrupt.getMessage()
                                + ", removing");
                cache.delete();
            }
        }

        // Create a new empty cache.
        directory.mkdirs();
        cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
        cache.rebuildJournal();
        return cache;
    }

    /**
     * 通过正文行记录，来填充用于存储Entry对象的集合
     * <p>
     * 1.读取journal文件的文件头部分，判断是否符合规范
     * <p>
     * 2.读取并处理正文部分
     * <p>
     * 3.计算多余的正文行数
     *
     * @throws IOException
     */
    private void readJournal() throws IOException {
        StrictLineReader reader = new StrictLineReader(new FileInputStream(journalFile), Util.US_ASCII);
        try {
            String magic = reader.readLine();
            String version = reader.readLine();
            String appVersionString = reader.readLine();
            String valueCountString = reader.readLine();
            String blank = reader.readLine();
            if (!MAGIC.equals(magic)
                    || !VERSION_1.equals(version)
                    || !Integer.toString(appVersion).equals(appVersionString)
                    || !Integer.toString(valueCount).equals(valueCountString)
                    || !"".equals(blank)) {
                throw new IOException("unexpected journal header: [" + magic + ", " + version + ", "
                        + valueCountString + ", " + blank + "]");
            }

            int lineCount = 0;
            while (true) {
                try {
                    readJournalLine(reader.readLine());
                    lineCount++;
                } catch (EOFException endOfJournal) {
                    break;
                }
            }
            redundantOpCount = lineCount - lruEntries.size();

            // If we ended on a truncated line, rebuild the journal before appending to it.
            if (reader.hasUnterminatedLine()) {
                rebuildJournal();
            } else {
                journalWriter = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(journalFile, true), Util.US_ASCII));
            }
        } finally {
            Util.closeQuietly(reader);
        }
    }

    /**
     * 处理一条正文行记录
     * <p>
     * 1.判断该正文行是否符合规范
     * <p>
     * 2.如果journal state key为REMOVE的话，那么就将其所对应的cache key从存储entry的集合中移除掉
     * <p>
     * 3.如果存储Entry对象的集合中没有该正文行中的cache key所对应的值，那么就新建一个Entry对象到集合中去
     * <p>
     * 4.根据该正文行格式，设置其Entry对象的类型（CLEAN/DIRTY/READ型）和属性
     *
     * @param line journal文件正文（除开头文件那五行）的某一行字符串
     * @throws IOException
     */
    private void readJournalLine(String line) throws IOException {
        int firstSpace = line.indexOf(' ');
        if (firstSpace == -1) {
            throw new IOException("unexpected journal line: " + line);
        }

        int keyBegin = firstSpace + 1;
        int secondSpace = line.indexOf(' ', keyBegin);
        final String key;
        if (secondSpace == -1) {
            key = line.substring(keyBegin);
            if (firstSpace == REMOVE.length() && line.startsWith(REMOVE)) {
                lruEntries.remove(key);
                return;
            }
        } else {
            key = line.substring(keyBegin, secondSpace);
        }

        Entry entry = lruEntries.get(key);
        if (entry == null) {
            entry = new Entry(key);
            lruEntries.put(key, entry);
        }

        if (secondSpace != -1 && firstSpace == CLEAN.length() && line.startsWith(CLEAN)) {
            String[] parts = line.substring(secondSpace + 1).split(" ");
            entry.readable = true;
            entry.currentEditor = null;
            entry.setLengths(parts);
        } else if (secondSpace == -1 && firstSpace == DIRTY.length() && line.startsWith(DIRTY)) {
            entry.currentEditor = new Editor(entry);
        } else if (secondSpace == -1 && firstSpace == READ.length() && line.startsWith(READ)) {
            // This work was already done by calling lruEntries.get().
        } else {
            throw new IOException("unexpected journal line: " + line);
        }
    }

    /**
     * Computes the initial size and collects garbage as a part of opening the
     * cache. Dirty entries are assumed to be inconsistent and will be deleted.
     * <p>
     * 存储Entry对象的集合只保留CLEAN型Entry对象
     * <p>
     * 1.将journal.tmp文件删除掉（如果有的话）
     * <p>
     * 2.记录所有CLEAN型Entry对象所对应的所有文件的个数和大小
     * <p>
     * 3.将集合中其它非CLEAN型Entry对象从集合中移除掉，并将这些被移除的对象所对应的文件也删除掉
     */
    private void processJournal() throws IOException {
        deleteIfExists(journalFileTmp);
        for (Iterator<Entry> i = lruEntries.values().iterator(); i.hasNext(); ) {
            Entry entry = i.next();
            if (entry.currentEditor == null) {
                for (int t = 0; t < valueCount; t++) {
                    size += entry.lengths[t];
                }
            } else {
                entry.currentEditor = null;
                for (int t = 0; t < valueCount; t++) {
                    deleteIfExists(entry.getCleanFile(t));
                    deleteIfExists(entry.getDirtyFile(t));
                }
                i.remove();
            }
        }
    }

    /**
     * Creates a new journal that omits redundant information. This replaces the
     * current journal if it exists.
     * <p>
     * 重构journal文件，并根据存储Entry对象的集合，来增加journal文件的正文行部分
     * <p>
     * 1.添加头文件部分
     * <p>
     * 2.根据用于存储Entry对象的集合，来添加正文行部分的记录
     * <p>
     * 3.若Entry对象的currentEditor属性值不为空，则添加一条DIRTY型记录，否则则添加一条CLEAN型记录
     * <p>
     * 注：以上三步都是写入到journal.tmp文件中去的
     * <p>
     * 4.若此时journal文件存在，则重命名为journal.bkp（如果journal.bkp文件存在，则会删除掉）文件，
     * 然后再将写好的journal.tmp文件重命名为journal文件，最后再将journal.bkp文件删除掉
     */
    private synchronized void rebuildJournal() throws IOException {
        if (journalWriter != null) {
            journalWriter.close();
        }

        Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(journalFileTmp), Util.US_ASCII));
        try {
            writer.write(MAGIC);
            writer.write("\n");
            writer.write(VERSION_1);
            writer.write("\n");
            writer.write(Integer.toString(appVersion));
            writer.write("\n");
            writer.write(Integer.toString(valueCount));
            writer.write("\n");
            writer.write("\n");

            for (Entry entry : lruEntries.values()) {
                if (entry.currentEditor != null) {
                    writer.write(DIRTY + ' ' + entry.key + '\n');
                } else {
                    writer.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
                }
            }
        } finally {
            writer.close();
        }

        if (journalFile.exists()) {
            renameTo(journalFile, journalFileBackup, true);
        }
        renameTo(journalFileTmp, journalFile, false);
        journalFileBackup.delete();

        journalWriter = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(journalFile, true), Util.US_ASCII));
    }

    /**
     * 删除文件（如果该文件存在的话）
     */
    private static void deleteIfExists(File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException();
        }
    }

    /**
     * 文件重命名
     */
    private static void renameTo(File from, File to, boolean deleteDestination) throws IOException {
        if (deleteDestination) {
            deleteIfExists(to);
        }
        if (!from.renameTo(to)) {
            throw new IOException();
        }
    }

    /**
     * Returns a snapshot of the entry named {@code key}, or null if it doesn't
     * exist is not currently readable. If a value is returned, it is moved to
     * the head of the LRU queue.
     * <p>
     * 为Entry对象创建一个对应的{@link Value}对象，并在journal文件中生成一条READ型记录。
     * 注：该Entry对象必须是CLEAN型的，否则返回null
     */
    public synchronized Value get(String key) throws IOException {
        checkNotClosed();
        Entry entry = lruEntries.get(key);
        if (entry == null) {
            return null;
        }

        if (!entry.readable) {
            return null;
        }

        for (File file : entry.cleanFiles) {
            // A file must have been deleted manually!
            if (!file.exists()) {
                return null;
            }
        }

        redundantOpCount++;
        journalWriter.append(READ);
        journalWriter.append(' ');
        journalWriter.append(key);
        journalWriter.append('\n');
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable);
        }

        return new Value(key, entry.sequenceNumber, entry.cleanFiles, entry.lengths);
    }

    /**
     * Returns an editor for the entry named {@code key}, or null if another
     * edit is in progress.
     */
    public Editor edit(String key) throws IOException {
        return edit(key, ANY_SEQUENCE_NUMBER);
    }

    /**
     * 给CLEAN型Entry对象的currentEditor属性设置一个Editor对象的值，并在journal文件中生成一条DIRTY型正文行记录
     * <p>
     * 疑问：为何这里生成了一条DIRTY型记录redundantOpCount却没自增
     */
    private synchronized Editor edit(String key, long expectedSequenceNumber) throws IOException {
        checkNotClosed();
        Entry entry = lruEntries.get(key);
        if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER && (entry == null
                || entry.sequenceNumber != expectedSequenceNumber)) {
            return null; // Value is stale.
        }
        if (entry == null) {
            entry = new Entry(key);
            lruEntries.put(key, entry);
        } else if (entry.currentEditor != null) {
            return null; // Another edit is in progress.
        }

        Editor editor = new Editor(entry);
        entry.currentEditor = editor;

        // Flush the journal before creating files to prevent file leaks.
        journalWriter.append(DIRTY);
        journalWriter.append(' ');
        journalWriter.append(key);
        journalWriter.append('\n');
        journalWriter.flush();
        return editor;
    }

    /**
     * Returns the directory where this cache stores its data.
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Returns the maximum number of bytes that this cache should use to store
     * its data.
     */
    public synchronized long getMaxSize() {
        return maxSize;
    }

    /**
     * Changes the maximum number of bytes the cache can store and queues a job
     * to trim the existing store, if necessary.
     */
    public synchronized void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
        executorService.submit(cleanupCallable);
    }

    /**
     * Returns the number of bytes currently being used to store the values in
     * this cache. This may be greater than the max size if a background
     * deletion is pending.
     */
    public synchronized long size() {
        return size;
    }

    /**
     * 用来整理一个Entry对象
     * <p>
     * 如果Entry对象为CLEAN型，则在journal文件中增加一条CLEAN型记录，并为该Entry对象
     * 的sequenceNumber属性赋值
     * <p>
     * 否则从集合中移除该Entry对象，并在journal文件中增加一条REMOVE型记录
     */
    private synchronized void completeEdit(Editor editor, boolean success) throws IOException {
        Entry entry = editor.entry;
        if (entry.currentEditor != editor) {
            throw new IllegalStateException();
        }

        // If this edit is creating the entry for the first time, every index must have a value.
        if (success && !entry.readable) {
            for (int i = 0; i < valueCount; i++) {
                if (!editor.written[i]) {
                    editor.abort();
                    throw new IllegalStateException("Newly created entry didn't create value for index " + i);
                }
                if (!entry.getDirtyFile(i).exists()) {
                    editor.abort();
                    return;
                }
            }
        }

        for (int i = 0; i < valueCount; i++) {
            File dirty = entry.getDirtyFile(i);
            if (success) {
                if (dirty.exists()) {
                    File clean = entry.getCleanFile(i);
                    dirty.renameTo(clean);
                    long oldLength = entry.lengths[i];
                    long newLength = clean.length();
                    entry.lengths[i] = newLength;
                    size = size - oldLength + newLength;
                }
            } else {
                deleteIfExists(dirty);
            }
        }

        redundantOpCount++;//增加了一条CLEAN或REMOVE型记录
        entry.currentEditor = null;
        if (entry.readable | success) {
            entry.readable = true;
            journalWriter.append(CLEAN);
            journalWriter.append(' ');
            journalWriter.append(entry.key);
            journalWriter.append(entry.getLengths());
            journalWriter.append('\n');

            if (success) {
                entry.sequenceNumber = nextSequenceNumber++;
            }
        } else {
            lruEntries.remove(entry.key);
            journalWriter.append(REMOVE);
            journalWriter.append(' ');
            journalWriter.append(entry.key);
            journalWriter.append('\n');
        }
        journalWriter.flush();

        if (size > maxSize || journalRebuildRequired()) {
            executorService.submit(cleanupCallable);
        }
    }

    /**
     * We only rebuild the journal when it will halve the size of the journal
     * and eliminate at least 2000 ops.
     * <p>
     * 当多余的正文行超过2000行，且超过了集合中Entry对象的个数时，返回true
     */
    private boolean journalRebuildRequired() {
        final int redundantOpCompactThreshold = 2000;
        return redundantOpCount >= redundantOpCompactThreshold //
                && redundantOpCount >= lruEntries.size();
    }

    /**
     * Drops the entry for {@code key} if it exists and can be removed. Entries
     * actively being edited cannot be removed.
     * <p>
     * 将一个CLEAN型Entry对象从集合中移除，并且删除其所对应的缓存文件；在journal文件中增加一条REMOVE记录
     *
     * @return true if an entry was removed.
     */
    public synchronized boolean remove(String key) throws IOException {
        checkNotClosed();
        Entry entry = lruEntries.get(key);
        if (entry == null || entry.currentEditor != null) {
            return false;
        }

        for (int i = 0; i < valueCount; i++) {
            File file = entry.getCleanFile(i);
            if (file.exists() && !file.delete()) {
                throw new IOException("failed to delete " + file);
            }
            size -= entry.lengths[i];
            entry.lengths[i] = 0;
        }

        redundantOpCount++;//增加了一条REMOVE型记录
        journalWriter.append(REMOVE);
        journalWriter.append(' ');
        journalWriter.append(key);
        journalWriter.append('\n');

        lruEntries.remove(key);

        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable);
        }

        return true;
    }

    /**
     * Returns true if this cache has been closed.
     */
    public synchronized boolean isClosed() {
        return journalWriter == null;
    }

    /**
     * 检测缓存是否未关闭
     */
    private void checkNotClosed() {
        if (journalWriter == null) {
            throw new IllegalStateException("cache is closed");
        }
    }

    /**
     * Force buffered operations to the filesystem.
     */
    public synchronized void flush() throws IOException {
        checkNotClosed();
        trimToSize();
        journalWriter.flush();
    }

    /**
     * Closes this cache. Stored values will remain on the filesystem.
     * <p>
     * 将存储Entry对象集合中的所有非CLEAN型（即READ型和DIRTY型）的记录移除，
     * 并将该集合所有对应的以“.tmp”结尾的文件删除掉；关闭文件流
     */
    public synchronized void close() throws IOException {
        if (journalWriter == null) {
            return; // Already closed.
        }
        for (Entry entry : new ArrayList<Entry>(lruEntries.values())) {
            if (entry.currentEditor != null) {
                entry.currentEditor.abort();
            }
        }
        trimToSize();
        journalWriter.close();
        journalWriter = null;
    }

    /**
     * 若缓存文件的总大小超过了配置的文件总大小，则重复执行删除队列头的那个缓存文件，
     * 直到缓存文件总大小小于等于配置的文件总大小
     */
    private void trimToSize() throws IOException {
        while (size > maxSize) {
            Map.Entry<String, Entry> toEvict = lruEntries.entrySet().iterator().next();
            remove(toEvict.getKey());
        }
    }

    /**
     * Closes the cache and deletes all of its stored values. This will delete
     * all files in the cache directory including files that weren't created by
     * the cache.
     * <p>
     * 关闭文件流，并将缓存文件目录下的所有文件删除掉
     */
    public void delete() throws IOException {
        close();
        Util.deleteContents(directory);
    }

    private static String inputStreamToString(InputStream in) throws IOException {
        return Util.readFully(new InputStreamReader(in, Util.UTF_8));
    }

    /**
     * A snapshot of the values for an entry.
     */
    public final class Value {
        private final String key;
        private final long sequenceNumber;
        private final long[] lengths;
        private final File[] files;

        private Value(String key, long sequenceNumber, File[] files, long[] lengths) {
            this.key = key;
            this.sequenceNumber = sequenceNumber;
            this.files = files;
            this.lengths = lengths;
        }

        /**
         * Returns an editor for this snapshot's entry, or null if either the
         * entry has changed since this snapshot was created or if another edit
         * is in progress.
         */
        public Editor edit() throws IOException {
            return DiskLruCache.this.edit(key, sequenceNumber);
        }

        public File getFile(int index) {
            return files[index];
        }

        /**
         * Returns the string value for {@code index}.
         */
        public String getString(int index) throws IOException {
            InputStream is = new FileInputStream(files[index]);
            return inputStreamToString(is);
        }

        /**
         * Returns the byte length of the value for {@code index}.
         */
        public long getLength(int index) {
            return lengths[index];
        }
    }

    /**
     * Edits the values for an entry.
     */
    public final class Editor {
        private final Entry entry;
        private final boolean[] written;//用来标识是否写入了文件
        private boolean committed;

        private Editor(Entry entry) {
            this.entry = entry;
            this.written = (entry.readable) ? null : new boolean[valueCount];
        }

        /**
         * Returns an unbuffered input stream to read the last committed value,
         * or null if no value has been committed.
         */
        private InputStream newInputStream(int index) throws IOException {
            synchronized (DiskLruCache.this) {
                if (entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                if (!entry.readable) {
                    return null;
                }
                try {
                    return new FileInputStream(entry.getCleanFile(index));
                } catch (FileNotFoundException e) {
                    return null;
                }
            }
        }

        /**
         * Returns the last committed value as a string, or null if no value
         * has been committed.
         */
        public String getString(int index) throws IOException {
            InputStream in = newInputStream(index);
            return in != null ? inputStreamToString(in) : null;
        }

        public File getFile(int index) throws IOException {
            synchronized (DiskLruCache.this) {
                if (entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                if (!entry.readable) {
                    written[index] = true;
                }
                File dirtyFile = entry.getDirtyFile(index);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                return dirtyFile;
            }
        }

        /**
         * Sets the value at {@code index} to {@code value}.
         */
        public void set(int index, String value) throws IOException {
            Writer writer = null;
            try {
                OutputStream os = new FileOutputStream(getFile(index));
                writer = new OutputStreamWriter(os, Util.UTF_8);
                writer.write(value);
            } finally {
                Util.closeQuietly(writer);
            }
        }

        /**
         * Commits this edit so it is visible to readers.  This releases the
         * edit lock so another edit may be started on the same key.
         * <p>
         * 将临时缓存文件（以“.tmp”为后缀）的后缀去掉，并在journal文件中增加一条CLEAN型记录
         */
        public void commit() throws IOException {
            // The object using this Editor must catch and handle any errors
            // during the write. If there is an error and they call commit
            // anyway, we will assume whatever they managed to write was valid.
            // Normally they should call abort.
            completeEdit(this, true);
            committed = true;
        }

        /**
         * Aborts this edit. This releases the edit lock so another edit may be
         * started on the same key.
         * <p>
         * 1.将以“.tmp”结尾的缓存文件删除掉（如果有的话）
         * <p>
         * 2.如果该Edit对象对应的Entry对象的readable属性值为true时，则在journal文件中增加一条CLEAN型记录，
         * 否则则增加一条REMOVE型记录
         * <p>
         * 当写入缓存文件失败时，调用该方法，在journal文件中新增一条REMOVE型记录，表示该图片需要重新写入
         */
        public void abort() throws IOException {
            completeEdit(this, false);
        }

        public void abortUnlessCommitted() {
            if (!committed) {
                try {
                    abort();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private final class Entry {
        private final String key;

        /**
         * Lengths of this entry's files.
         */
        private final long[] lengths;

        /**
         * Memoized File objects for this entry to avoid char[] allocations.
         */
        File[] cleanFiles;
        File[] dirtyFiles;

        /**
         * True if this entry has ever been published.
         * <p>
         * 曾经被发布过 那它的值就是true
         * <p>
         * 若其值为true，则该Entry对应的journal state key为CLEAN，且该Entry的currentEditor对象必为null
         */
        private boolean readable;

        /**
         * The ongoing edit or null if this entry is not being edited.
         * <p>
         * 这个Entry对象对应的Editor对象
         * <p>
         * 若该属性值不为空，则该Entry对象所对应的正文行必为DIRTY型
         */
        private Editor currentEditor;

        /**
         * The sequence number of the most recently committed edit to this entry.
         * <p>
         * 最近编辑它的序列号
         */
        private long sequenceNumber;

        private Entry(String key) {
            this.key = key;
            this.lengths = new long[valueCount];
            cleanFiles = new File[valueCount];
            dirtyFiles = new File[valueCount];

            // The names are repetitive so re-use the same builder to avoid allocations.
            StringBuilder fileBuilder = new StringBuilder(key).append('.');
            int truncateTo = fileBuilder.length();
            for (int i = 0; i < valueCount; i++) {
                fileBuilder.append(i);
                cleanFiles[i] = new File(directory, fileBuilder.toString());
                fileBuilder.append(".tmp");
                dirtyFiles[i] = new File(directory, fileBuilder.toString());
                fileBuilder.setLength(truncateTo);
            }
        }

        public String getLengths() throws IOException {
            StringBuilder result = new StringBuilder();
            for (long size : lengths) {
                result.append(' ').append(size);
            }
            return result.toString();
        }

        /**
         * Set lengths using decimal numbers like "10123".
         */
        private void setLengths(String[] strings) throws IOException {
            if (strings.length != valueCount) {
                throw invalidLengths(strings);
            }

            try {
                for (int i = 0; i < strings.length; i++) {
                    lengths[i] = Long.parseLong(strings[i]);
                }
            } catch (NumberFormatException e) {
                throw invalidLengths(strings);
            }
        }

        private IOException invalidLengths(String[] strings) throws IOException {
            throw new IOException("unexpected journal line: " + java.util.Arrays.toString(strings));
        }

        /**
         * Entry对象为CLEAN型时，其所对应的缓存文件通过该方法获取
         */
        public File getCleanFile(int i) {
            return cleanFiles[i];
        }

        /**
         * Entry对象为DIRTY型时，其所对应的文件包含两个，一个通过{@link #getCleanFile}获取，
         * 另一个则是通过{@link #getDirtyFile}获取
         */
        public File getDirtyFile(int i) {
            return dirtyFiles[i];
        }
    }

    /**
     * A {@link ThreadFactory} that builds a thread with a specific thread name
     * and with minimum priority.
     */
    private static final class DiskLruCacheThreadFactory implements ThreadFactory {
        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread result = new Thread(runnable, "glide-disk-lru-cache-thread");
            result.setPriority(Thread.MIN_PRIORITY);
            return result;
        }
    }
}
