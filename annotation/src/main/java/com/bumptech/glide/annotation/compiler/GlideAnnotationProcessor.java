package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideType;
import com.google.auto.service.AutoService;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

// Links in Javadoc will work due to build setup, even though there is no direct dependency here.

/**
 * Generates classes based on Glide's annotations that configure Glide, add support for additional
 * resource types, and/or extend Glide's API.
 * <p>
 * <p>This processor discovers all {@link com.bumptech.glide.module.RootGlideModule} and
 * {@link com.bumptech.glide.module.ChildGlideModule} implementations that are
 * annotated with {@link com.bumptech.glide.annotation.GlideModule}. Any implementations missing the
 * annotation will be ignored.
 * <p>
 * <p>This processor also discovers all {@link com.bumptech.glide.annotation.GlideExtension}
 * annotated classes.
 * <p>
 * <p>Multiple classes are generated by this processor:
 * <ul>
 * <li>For {@link com.bumptech.glide.module.ChildGlideModule}s - A GlideIndexer class in a
 * specific package that will later be used by the processor to discover all
 * {@link com.bumptech.glide.module.ChildGlideModule} classes.
 * <li>For {@link com.bumptech.glide.module.RootGlideModule}s - A single
 * {@link com.bumptech.glide.module.RootGlideModule} implementation
 * ({@link com.bumptech.glide.GeneratedRootGlideModule}) that calls all
 * {@link com.bumptech.glide.module.ChildGlideModule}s and the
 * original {@link com.bumptech.glide.module.RootGlideModule} in the correct order when Glide is
 * initialized.
 * <li>{@link com.bumptech.glide.annotation.GlideExtension}s -
 * <ul>
 * <li>A {@link com.bumptech.glide.request.BaseRequestOptions} implementation that contains
 * static versions of all builder methods in the base class and both static and instance
 * versions of methods in all {@link com.bumptech.glide.annotation.GlideExtension}s.
 * <li>If one or more methods in one or more
 * {@link com.bumptech.glide.annotation.GlideExtension} annotated classes are annotated with
 * {@link GlideType}:
 * <ul>
 * <li>A {@link com.bumptech.glide.RequestManager} implementation containing a generated
 * method for each method annotated with
 * {@link GlideType}.
 * <li>A {@link com.bumptech.glide.manager.RequestManagerRetriever.RequestManagerFactory}
 * implementation that produces the generated {@link com.bumptech.glide.RequestManager}s.
 * <li>A {@link com.bumptech.glide.Glide} look-alike that implements all static methods in
 * the {@link com.bumptech.glide.Glide} singleton and returns the generated
 * {@link com.bumptech.glide.RequestManager} implementation when appropriate.
 * </ul>
 * </ul>
 * </ul>
 * <p>
 * <p>{@link com.bumptech.glide.module.RootGlideModule} implementations must only be included in
 * applications, not in libraries. There must be exactly one
 * {@link com.bumptech.glide.module.RootGlideModule} implementation per
 * Application. The {@link com.bumptech.glide.module.RootGlideModule} class is
 * used as a signal that all modules have been found and that the final merged
 * {@link com.bumptech.glide.GeneratedRootGlideModule} impl can be created.
 */
@AutoService(Processor.class)
public final class GlideAnnotationProcessor extends AbstractProcessor {
    static final boolean DEBUG = false;
    private ProcessorUtil processorUtil;
    private ChildModuleProcessor childModuleProcessor;
    private RootModuleProcessor rootModuleProcessor;
    private boolean isGeneratedRootGlideModuleWritten;
    private ExtensionProcessor extensionProcessor;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        processorUtil = new ProcessorUtil(processingEnvironment);
        IndexerGenerator indexerGenerator = new IndexerGenerator(processorUtil);
        childModuleProcessor = new ChildModuleProcessor(processorUtil, indexerGenerator);
        rootModuleProcessor = new RootModuleProcessor(processingEnvironment, processorUtil);
        extensionProcessor = new ExtensionProcessor(processorUtil, indexerGenerator);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> result = new HashSet<>();
        result.addAll(childModuleProcessor.getSupportedAnnotationTypes());
        result.addAll(extensionProcessor.getSupportedAnnotationTypes());
        return result;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    /**
     * Each round we do the following:
     * <ol>
     * <li>Find all RootGlideModules and save them to an instance variable (throw if > 1).
     * <li>Find all ChildGlideModules
     * <li>For each ChildGlideModule, write an Indexer with an Annotation with the class name.
     * <li>If we wrote any Indexers, return and wait for the next round.
     * <li>If we didn't write any Indexers and there is a RootGlideModule, write the
     * GeneratedRootGlideModule. Once the GeneratedRootGlideModule is written, we expect to be
     * finished. Any further generation of related classes will result in errors.
     * </ol>
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        processorUtil.process();
        boolean newModulesWritten = childModuleProcessor.processModules(set, env);
        boolean newExtensionWritten = extensionProcessor.processExtensions(set, env);
        rootModuleProcessor.processModules(set, env);

        if (newExtensionWritten || newModulesWritten) {
            if (isGeneratedRootGlideModuleWritten) {
                throw new IllegalStateException("Cannot process annotations after writing RootGlideModule");
            }
            return true;
        }

        if (!isGeneratedRootGlideModuleWritten) {
            isGeneratedRootGlideModuleWritten = rootModuleProcessor.maybeWriteRootModule();
        }
        return true;
    }
}