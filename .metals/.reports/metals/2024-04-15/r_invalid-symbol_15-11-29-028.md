### scala.meta.internal.mtags.IndexingExceptions$InvalidSymbolException: //#

Symbol: //#

#### Error stacktrace:

```
scala.meta.internal.mtags.OnDemandSymbolIndex.definitions(OnDemandSymbolIndex.scala:61)
	scala.meta.internal.metals.DestinationProvider.definition(DefinitionProvider.scala:467)
	scala.meta.internal.metals.DestinationProvider.fromSymbol(DefinitionProvider.scala:505)
	scala.meta.internal.metals.DestinationProvider.fromSymbol(DefinitionProvider.scala:546)
	scala.meta.internal.metals.DefinitionProvider.fromSymbol(DefinitionProvider.scala:199)
	scala.meta.internal.metals.StacktraceAnalyzer.findLocationForSymbol$1(StacktraceAnalyzer.scala:67)
	scala.meta.internal.metals.StacktraceAnalyzer.$anonfun$fileLocationFromLine$2(StacktraceAnalyzer.scala:72)
	scala.PartialFunction$Unlifted.applyOrElse(PartialFunction.scala:347)
	scala.collection.IterableOnceOps.collectFirst(IterableOnce.scala:1142)
	scala.collection.IterableOnceOps.collectFirst$(IterableOnce.scala:1134)
	scala.collection.AbstractIterable.collectFirst(Iterable.scala:933)
	scala.meta.internal.metals.StacktraceAnalyzer.$anonfun$fileLocationFromLine$1(StacktraceAnalyzer.scala:72)
	scala.Option.flatMap(Option.scala:283)
	scala.meta.internal.metals.StacktraceAnalyzer.fileLocationFromLine(StacktraceAnalyzer.scala:70)
	scala.meta.internal.metals.debug.DebugProxy.$anonfun$handleServerMessage$1(DebugProxy.scala:240)
	scala.meta.internal.metals.debug.DebugProxy.$anonfun$handleServerMessage$1$adapted(DebugProxy.scala:207)
	scala.meta.internal.metals.debug.ServerAdapter.$anonfun$onReceived$1(ServerAdapter.scala:25)
	scala.meta.internal.metals.debug.MessageIdAdapter.$anonfun$listen$1(MessageIdAdapter.scala:57)
	org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer.handleMessage(StreamMessageProducer.java:194)
	org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer.listen(StreamMessageProducer.java:94)
	scala.meta.internal.metals.debug.SocketEndpoint.listen(SocketEndpoint.scala:26)
	scala.meta.internal.metals.debug.MessageIdAdapter.listen(MessageIdAdapter.scala:47)
	scala.meta.internal.metals.debug.ServerAdapter.onReceived(ServerAdapter.scala:18)
	scala.meta.internal.metals.debug.DebugProxy.$anonfun$listenToServer$1(DebugProxy.scala:78)
	scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
	scala.concurrent.Future$.$anonfun$apply$1(Future.scala:687)
	scala.concurrent.impl.Promise$Transformation.run(Promise.scala:467)
	java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	java.lang.Thread.run(Thread.java:750)
```
#### Short summary: 

scala.meta.internal.mtags.IndexingExceptions$InvalidSymbolException: //#