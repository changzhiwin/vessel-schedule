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