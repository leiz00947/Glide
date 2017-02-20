# Glide
图片加载框架

写在前面
	在Glide 3.7.0中默认解码图片质量是ARGB_8888

com.bumptech.glide.Registry
	-> com.bumptech.glide.load.model.ModelLoaderRegistry
	-> com.bumptech.glide.provider.EncoderRegistry
	-> com.bumptech.glide.provider.ResourceDecoderRegistry
	-> com.bumptech.glide.provider.ResourceEncoderRegistry
	-> com.bumptech.glide.load.data.DataRewinderRegistry
	-> com.bumptech.glide.load.resource.transcode.TranscoderRegistry
	-> com.bumptech.glide.provider.ImageHeaderParserRegistry
	Registry中的append和register方法是将传参数据添加到以上六个对象中的集合中去。

	ModelLoaderRegistry中的集合数据有：
	01)new MultiModelLoaderFactory.Entry(GifDecoder.class, GifDecoder.class, new UnitModelLoader.Factory<GifDecoder>())
	02)new MultiModelLoaderFactory.Entry(File.class, ByteBuffer.class, new ByteBufferFileLoader.Factory())
	03)new MultiModelLoaderFactory.Entry(File.class, InputStream.class, new FileLoader.StreamFactory())
	04)new MultiModelLoaderFactory.Entry(File.class, ParcelFileDescriptor.class, new FileLoader.FileDescriptorFactory())
	05)new MultiModelLoaderFactory.Entry(File.class, File.class, new UnitModelLoader.Factory<File>())
	06)new MultiModelLoaderFactory.Entry(int.class, InputStream.class, new ResourceLoader.StreamFactory(resources))
	07)new MultiModelLoaderFactory.Entry(int.class, ParcelFileDescriptor.class, new ResourceLoader.FileDescriptorFactory(resources))
	08)new MultiModelLoaderFactory.Entry(Integer.class, InputStream.class, new ResourceLoader.StreamFactory(resources))
	09)new MultiModelLoaderFactory.Entry(Integer.class, ParcelFileDescriptor.class, new ResourceLoader.FileDescriptorFactory(resources))
	10)new MultiModelLoaderFactory.Entry(String.class, InputStream.class, new DataUrlLoader.StreamFactory())
	11)new MultiModelLoaderFactory.Entry(String.class, InputStream.class, new StringLoader.StreamFactory())
	12)new MultiModelLoaderFactory.Entry(String.class, ParcelFileDescriptor.class, new StringLoader.FileDescriptorFactory())
	13)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new HttpUriLoader.Factory())
	14)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new AssetUriLoader.StreamFactory(context.getAssets()))
	15)new MultiModelLoaderFactory.Entry(Uri.class, ParcelFileDescriptor.class, new AssetUriLoader.FileDescriptorFactory(context.getAssets()))
	16)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new MediaStoreImageThumbLoader.Factory(context))
	17)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new MediaStoreVideoThumbLoader.Factory(context))
	18)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new UriLoader.StreamFactory(context.getContentResolver()))
	19)new MultiModelLoaderFactory.Entry(Uri.class, ParcelFileDescriptor.class, new UriLoader.FileDescriptorFactory(context.getContentResolver()))
	20)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new UrlUriLoader.StreamFactory())
	21)new MultiModelLoaderFactory.Entry(URL.class, InputStream.class, new UrlLoader.StreamFactory())
	22)new MultiModelLoaderFactory.Entry(Uri.class, File.class, new MediaStoreFileLoader.Factory(context))
	23)new MultiModelLoaderFactory.Entry(GlideUrl.class, InputStream.class, new HttpGlideUrlLoader.Factory())
	24)new MultiModelLoaderFactory.Entry(byte[].class, ByteBuffer.class, new ByteArrayLoader.ByteBufferFactory())
	25)new MultiModelLoaderFactory.Entry(byte[].class, InputStream.class, new ByteArrayLoader.StreamFactory())

	EncoderRegistry中的集合数据有：
	1)new EncoderRegistry.Entry(ByteBuffer.class, new ByteBufferEncoder)
	2)new EncoderRegistry.Entry(InputStream.class, new StreamEncoder(arrayPool))

	ResourceDecoderRegistry中的集合数据有：
	+)new ResourceDecoderRegistry.Entry(ByteBuffer.class, GifDrawable.class, byteBufferGifDecoder)
	+)new ResourceDecoderRegistry.Entry(InputStream.class, GifDrawable.class, new StreamGifDecoder(byteBufferGifDecoder, arrayPool))
	1)new ResourceDecoderRegistry.Entry(ByteBuffer.class, Bitmap.class, new ByteBufferBitmapDecoder(downsampler))
	2)new ResourceDecoderRegistry.Entry(InputStream.class, Bitmap.class, new StreamBitmapDecoder(downsampler,arrayPool))
	3)new ResourceDecoderRegistry.Entry(ParcelFileDescriptor.class, Bitmap.class, new VideoBitmapDecoder(bitmapPool))
	4)new ResourceDecoderRegistry.Entry(ByteBuffer.class, BitmapDrawable.class, new BitmapDrawableDecoder<>(resources, bitmapPool, new ByteBufferBitmapDecoder(downsampler)))
	5)new ResourceDecoderRegistry.Entry(InputStream.class, BitmapDrawable.class, new BitmapDrawableDecoder<>(resources, bitmapPool, new StreamBitmapDecoder(downsampler, arrayPool))
	6)new ResourceDecoderRegistry.Entry(ParcelFileDescriptor.class, BitmapDrawable.class, new BitmapDrawableDecoder<>(resources, bitmapPool, new VideoBitmapDecoder(bitmapPool)))
	7)new ResourceDecoderRegistry.Entry(GifDecoder.class, Bitmap.class, new GifFrameResourceDecoder(bitmapPool))
	8)new ResourceDecoderRegistry.Entry(File.class, File.class, new FileDecoder())

	ResourceEncoderRegistry中的集合数据有：
	1)new ResourceEncoderRegistry.Entry(Bitmap.class, new BitmapEncoder())
	2)new ResourceEncoderRegistry.Entry(BitmapDrawable.class, new BitmapDrawableEncoder(bitmapPool, new BitmapEncoder()))
	3)new ResourceEncoderRegistry.Entry(GifDrawable.class, new GifDrawableEncoder())

	DataRewinderRegistry中的集合数据有：
	*)<ByteBuffer.class, new ByteBufferRewinder.Factory()>
	*)<InputStream.class, new InputStreamRewinder.Factory(arrayPool)>

	TranscoderRegistry中的集合数据有：
	1)new TranscoderRegistry.Entry(Bitmap.class, BitmapDrawable.class, new BitmapDrawableTranscoder(resources, bitmapPool))
	2)new TranscoderRegistry.Entry(Bitmap.class, byte[].class, new BitmapBytesTranscoder())
	3)new TranscoderRegistry.Entry(GifDrawable.class, byte[].class, new GifDrawableBytesTranscoder())

	ImageHeaderParserRegistry中的集合数据有：
	1)new DefaultImageHeaderParser()

	调用com.bumptech.glide.load.engine.DecodeHelper<Transcode>#getLoadData()，图片来源为远程网络的String字符串对象：
	1.在ModelLoaderRegistry数据集合中找出Entry第一个传参为String的数据项，通过上面的数据集合可以知道，符合条件的有
		(1)new MultiModelLoaderFactory.Entry(String.class, InputStream.class, new DataUrlLoader.StreamFactory())
		(2)new MultiModelLoaderFactory.Entry(String.class, InputStream.class, new StringLoader.StreamFactory())
		(3)new MultiModelLoaderFactory.Entry(String.class, ParcelFileDescriptor.class, new StringLoader.FileDescriptorFactory())
	2.但由于DataUrlLoader#handles(String)大多数情况可能返回false，实际符合条件的也就(2)和(3)两项
	3.而在DataUrlLoader.StreamFactory#build(MultiModelLoaderFactory)中调用了MultiModelLoaderFactory.build(Uri.class, InputStream.class)
	4.那么，在ModelLoaderRegistry数据集合中找出Entry第一个传参为Uri，第二个传参为InputStream.class的数据项，符合条件的有
		(1)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new HttpUriLoader.Factory())
		(2)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new AssetUriLoader.StreamFactory(context.getAssets()))
		(3)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new MediaStoreImageThumbLoader.Factory(context))
		(4)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new MediaStoreVideoThumbLoader.Factory(context))
		(5)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new UriLoader.StreamFactory(context.getContentResolver()))
		(6)new MultiModelLoaderFactory.Entry(Uri.class, InputStream.class, new UrlUriLoader.StreamFactory())

com.bumptech.glide.load.model.ModelLoader<Model,Data>
	所有已知实现类：
	-> com.bumptech.glide.load.model.AssetUriLoader<Data>
	-> com.bumptech.glide.load.model.stream.BaseGlideUrlLoader<Model>
	-> com.bumptech.glide.load.model.ByteArrayLoader<Data>
	-> com.bumptech.glide.load.model.ByteBufferFileLoader
	-> com.bumptech.glide.load.model.DataUrlLoader<Data>
	-> com.bumptech.glide.load.model.FileLoader<Data>
	-> com.bumptech.glide.load.model.stream.HttpGlideUrlLoader
	-> com.bumptech.glide.load.model.stream.HttpUriLoader
	-> com.bumptech.glide.load.model.MediaStoreFileLoader
	-> com.bumptech.glide.load.model.stream.MediaStoreImageThumbLoader
	-> com.bumptech.glide.load.model.stream.MediaStoreVideoThumbLoader
	-> com.bumptech.glide.load.model.MultiModelLoader<Model,Data>
	-> com.bumptech.glide.load.model.MultiModelLoaderFactory.EmptyModelLoader
	-> com.bumptech.glide.load.model.ResourceLoader<Data>
	-> com.bumptech.glide.load.model.StringLoader<Data>
	-> com.bumptech.glide.load.model.UnitModelLoader<Model>
	-> com.bumptech.glide.load.model.UriLoader<Data>
	-> com.bumptech.glide.load.model.stream.UrlLoader
	-> com.bumptech.glide.load.model.UrlUriLoader<Data>
	接口功能说明：1.创建一个带DataFetcher对象的LoadData实例；2.判断图片地址是否符合常规格式

com.bumptech.glide.load.Option<T>
	-> com.bumptech.glide.load.model.stream.HttpGlideUrlLoader.TIMEOUT
	-> com.bumptech.glide.load.resource.bitmap.BitmapEncoder.COMPRESSION_QUALITY
	-> com.bumptech.glide.load.resource.bitmap.BitmapEncoder.COMPRESSION_FORMAT
	-> com.bumptech.glide.load.resource.bitmap.Downsampler.DECODE_FORMAT
	-> com.bumptech.glide.load.resource.bitmap.Downsampler.DOWNSAMPLE_STRATEGY
	-> com.bumptech.glide.load.resource.bitmap.VideoBitmapDecode.TARGET_FRAME
	-> com.bumptech.glide.load.resource.bitmap.VideoBitmapDecode.FRAME_OPTION
	-> com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder.DISABLE_ANIMATION
	-> com.bumptech.glide.load.resource.gif.StreamGifDecoder.DISABLE_ANIMATION

com.bumptech.glide.load.data.DataFetcher<T>
	所有已知实现类：
	-> com.bumptech.glide.load.data.AssetPathFetcher<T>
	-> com.bumptech.glide.load.model.ByteArrayLoader.Fetcher<Data>
	-> com.bumptech.glide.load.model.ByteBufferFileLoader.ByteBufferFetcher
	-> com.bumptech.glide.load.model.DataUrlLoader.DataUriFetcher<Data>
	-> com.bumptech.glide.load.data.FileDescriptorAssetPathFetcher
	-> com.bumptech.glide.load.data.FileDescriptorLocalUriFetcher
	-> com.bumptech.glide.load.model.FileLoader.FileFetcher<Data>
	-> com.bumptech.glide.load.data.HttpUrlFetcher
	-> com.bumptech.glide.load.data.LocalUriFetcher<T>
	-> com.bumptech.glide.load.model.MediaStoreFileLoader.FilePathFetcher
	-> com.bumptech.glide.load.model.MultiModelLoader.MultiFetcher<Data>
	-> com.bumptech.glide.load.data.StreamAssetPathFetcher
	-> com.bumptech.glide.load.data.StreamLocalUriFetcher
	-> com.bumptech.glide.load.data.mediastore.ThumbFetcher
	-> com.bumptech.glide.load.model.UnitModelLoader.UnitFetcher<Model>
	接口功能说明：用来进行加载资源数据操作，然后关闭流操作接口，提供取消操作的接口，以及获取数据来源

com.bumptech.glide.load.engine.Resource<Z>
	所有已知实现类：
	-> com.bumptech.glide.load.resource.bitmap.BitmapDrawableResource
	-> com.bumptech.glide.load.resource.bitmap.BitmapResource
	-> com.bumptech.glide.load.resource.bytes.BytesResource
	-> com.bumptech.glide.load.resource.drawable.DrawableResource<T extends android.graphics.drawable.Drawable>
	-> com.bumptech.glide.load.engine.EngineResource<Z>
	-> com.bumptech.glide.load.resource.file.FileResource
	-> com.bumptech.glide.load.resource.gif.GifDrawableResource
	-> com.bumptech.glide.load.resource.bitmap.LazyBitmapDrawableResource
	-> com.bumptech.glide.load.engine.LockedResource<Z>
	-> com.bumptech.glide.load.resource.SimpleResource<T>
	接口功能说明：用来获取资源类型、资源对象、资源大小以及资源释放的操作

com.bumptech.glide.load.Transformation<T>
	所有已知实现类：
	-> com.bumptech.glide.load.resource.bitmap.BitmapDrawableTransformation
	-> com.bumptech.glide.load.resource.bitmap.BitmapTransformation
	-> com.bumptech.glide.load.resource.bitmap.CenterCrop
	-> com.bumptech.glide.load.resource.bitmap.CenterInside
	-> com.bumptech.glide.load.resource.bitmap.CircleCrop
	-> com.bumptech.glide.load.resource.bitmap.FitCenter
	-> com.bumptech.glide.load.resource.gif.GifDrawableTransformation
	-> com.bumptech.glide.load.MultiTransformation<T>
	-> com.bumptech.glide.load.resource.bitmap.RoundedCorners
	-> com.bumptech.glide.load.resource.UnitTransformation<T>
	接口功能说明：对图片进行外形上的调整，比如缩放，圆形裁剪等
	实际上，起作用的是CenterCrop、CenterInside、CircleCrop、FitCenter和RoundedCorners

com.bumptech.glide.request.transition.Transition<R>
	所有已知实现类：
	-> com.bumptech.glide.request.transition.BitmapContainerTransitionFactory.BitmapGlideAnimation
	-> com.bumptech.glide.request.transition.DrawableCrossFadeTransition
	-> com.bumptech.glide.request.transition.NoTransition<R>
	-> com.bumptech.glide.request.transition.ViewPropertyTransition<R>
	-> com.bumptech.glide.request.transition.ViewTransition<R>
	接口功能说明：对图片加载进行过渡的操作，包括过渡动画、渐变

com.bumptech.glide.TransitionOptions<CHILD extends TransitionOptions<CHILD,TranscodeType>,TranscodeType>
	所有已知子类：
	com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
	com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
	com.bumptech.glide.GenericTransitionOptions<TranscodeType>

com.bumptech.glide.request.target.Target<R>
	所有已知实现类：
	-> com.bumptech.glide.request.target.AppWidgetTarget
	-> com.bumptech.glide.request.target.BaseTarget<Z>
	-> com.bumptech.glide.request.target.BitmapImageViewTarget
	-> com.bumptech.glide.request.target.BitmapThumbnailImageViewTarget
	-> com.bumptech.glide.request.target.DrawableImageViewTarget
	-> com.bumptech.glide.request.target.DrawableThumbnailImageViewTarget
	-> com.bumptech.glide.load.resource.gif.GifFrameLoader.DelayTarget
	-> com.bumptech.glide.request.target.ImageViewTarget<Z>
	-> com.bumptech.glide.ListPreloader.PreloadTarget
	-> com.bumptech.glide.request.target.NotificationTarget
	-> com.bumptech.glide.request.target.PreloadTarget<Z>
	-> com.bumptech.glide.request.RequestFutureTarget<R>
	-> com.bumptech.glide.RequestManager.ClearTarget
	-> com.bumptech.glide.request.target.SimpleTarget<Z>
	-> com.bumptech.glide.request.target.ThumbnailImageViewTarget<T>
	-> com.bumptech.glide.util.ViewPreloadSizeProvider.SizeViewTarget
	-> com.bumptech.glide.request.target.ViewTarget<T extends android.view.View,Z>
	接口功能说明：1.图片加载状态的过程（如加载失败、加载成功等）；2.通知图片宽高值变化的接口；3.设置获取Request

缓存包（com.bumptech.glide.load.engine.cache）
	磁盘缓存com.bumptech.glide.load.engine.cache.DiskCache：
		1.SDCard的“Android/data/{应用包名}/cache”临时文件目录下（ExternalCacheDiskCacheFactory）
		2.“data/data/{应用包名}/cache”目录下（InternalCacheDiskCacheFactory）
	内存缓存com.bumptech.glide.load.engine.cache.MemoryCache：LruResourceCache
	com.bumptech.glide.load.engine.cache.MemorySizeCalculator用来基于不同设备来进行内存分配

池
	com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool：图片池

com.bumptech.glide.load.ResourceDecoder<T,Z>
	所有已知实现类：
	-> com.bumptech.glide.load.resource.bitmap.BitmapDrawableDecoder<DataType>
	-> com.bumptech.glide.load.resource.bitmap.ByteBufferBitmapDecoder
	-> com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder
	-> com.bumptech.glide.load.resource.file.FileDecoder
	-> com.bumptech.glide.load.resource.gif.GifFrameResourceDecoder
	-> com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder
	-> com.bumptech.glide.load.resource.gif.StreamGifDecoder
	-> com.bumptech.glide.load.resource.bitmap.VideoBitmapDecoder
	接口功能说明：1.判断资源能否被解码；2.对资源进行解码操作
	com.bumptech.glide.load.resource.bitmap.Downsampler用来进行图片解码操作的一些算法

com.bumptech.glide.load.Encoder<T>
	所有已知实现类：
	-> com.bumptech.glide.load.resource.bitmap.BitmapDrawableEncoder
	-> com.bumptech.glide.load.resource.bitmap.BitmapEncoder
	-> com.bumptech.glide.load.model.ByteBufferEncoder
	-> com.bumptech.glide.load.resource.gif.GifDrawableEncoder
	-> com.bumptech.glide.load.model.StreamEncoder
	ResourceDecoder主要是对图片进行缩放加工，而Encoder则主要是写入数据

InputStream的扩展类
	-> com.bumptech.glide.util.ExceptionCatchingInputStream
	-> com.bumptech.glide.util.MarkEnforcingInputStream
	-> com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream

com.bumptech.glide.request.Request
	com.bumptech.glide.RequestManager
	com.bumptech.glide.RequestBuilder<TranscodeType>
	com.bumptech.glide.manager.RequestManagerRetriever 主要用来创建获取RequestManager
	com.bumptech.glide.manager.RequestTracker 主要用来管理Request的开始和暂停，以及延迟处理
	com.bumptech.glide.request.SingleRequest<R>
	com.bumptech.glide.request.ThumbnailRequestCoordinator
	com.bumptech.glide.request.RequestOptions
	com.bumptech.glide.request.BaseRequestOptions<CHILD extends BaseRequestOptions<CHILD>>
	通过RequestTracker来调用Request的begin方法
	执行SingleRequest的begin方法时会调用Engine的load方法
	用RequestBuilder来创建并执行一个Request（SingleRequest或ThumbnailRequestCoordinator）
	用RequestManager来获取RequestBuilder和RequestOptions对象的引用
	RequestBuilder ——> RequestManager ——> RequestTracker

com.bumptech.glide.load.engine.Engine
	在GlideBuilder中实例化Engine
	Engine中创建EngineJob和DecodeJob实例

com.bumptech.glide.load.engine.DiskCacheStrategy

com.bumptech.glide.util.pool.FactoryPools

三个onResourceReady调用的先后顺序为：
	1.com.bumptech.glide.request.ResourceCallback#onResourceReady(Resource<?>, DataSource)
	2.com.bumptech.glide.request.target.Target<R>#onResourceReady(R, Transition<? super R>)
	3.com.bumptech.glide.request.RequestListener<R>#onResourceReady(R, Object, Target<R>, DataSource, boolean)

com.bumptech.glide.load.data.DataFetcher.DataCallback<T>
	所有已知实现类：
	com.bumptech.glide.load.engine.DataCacheGenerator
	com.bumptech.glide.load.engine.ResourceCacheGenerator
	com.bumptech.glide.load.engine.SourceGenerator

com.bumptech.glide.module.GlideModule
	该接口中提供了两个需要实现的方法：applyOptions(Context, GlideBuilder)和registerComponents(Context, Registry)
	要将实现该接口的类在AndroidManifest.xml中注册，若工程进行了代码混淆，需要保留该实现类不被混淆
	在AndroidManifest.xml中注册的该接口的实现类中可以在applyOptions(Context, GlideBuilder)内进行GlideBuilder提供的公共接口的操作，比如修改DecodeFormat的图片解码质量等

com.bumptech.glide.load.engine.prefill预填充包
	com.bumptech.glide.load.engine.prefill.BitmapPreFiller
	com.bumptech.glide.load.engine.prefill.BitmapPreFillRunner
	com.bumptech.glide.load.engine.prefill.PreFillQueue
	com.bumptech.glide.load.engine.prefill.PreFillType

com.bumptech.glide.Glide#with(Context)
	com.bumptech.glide.manager.RequestManagerRetriever
	com.bumptech.glide.manager.RequestManagerFragment
	com.bumptech.glide.manager.RequestManagerTreeNode
	com.bumptech.glide.manager.SupportRequestManagerFragment

运行流程
	1)com.bumptech.glide.request.SingleRequest#onSizeReady(int, int)(com.bumptech.glide.request.target.SizeReadyCallback)
	2)com.bumptech.glide.load.engine.Engine#load(GlideContext, Object, Key, int, int, Class<?>, Class<R>, Priority, DiskCacheStrategy, Map<Class<?>, Transformation<?>>, boolean, Options, boolean, boolean, boolean, ResourceCallback)
	+)com.bumptech.glide.load.engine.EngineJob#start(DecodeJob<R>)
	+)com.bumptech.glide.load.engine.DecodeJob#run()
	3)com.bumptech.glide.request.SingleRequest#onResourceReady(Resource<?>, DataSource)(com.bumptech.glide.request.ResourceCallback)