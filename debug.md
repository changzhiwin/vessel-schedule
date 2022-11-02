#### beautifyErrors fix
```
Middleware.interceptPatch { req =>
  println(s"-------->> @@ req.accept: ${req.accept}")
  println(s"-------->> @@ req.userAgent: ${req.userAgent}")
  (req, "ok")
} { case (response, (request, _)) => 
  println(s"-------->> @@ response.status.isError: ${response.status.isError}")
  Patch.empty 
}
```

#### Error Handing

##### Network Error
```
[info] Stream application closed! Fail(io.netty.channel.AbstractChannel$AnnotatedConnectException: Connection refused: /127.0.0.1:4427,Stack trace for thread "zio-fiber-":
```

##### Application Error
```
[info] Stream application closed! Fail(cc.knowship.subscribe.SubscribeException$JsonDecodeFailed: NpediScheduleInfoReply,Stack trace for thread "zio-fiber-":
```