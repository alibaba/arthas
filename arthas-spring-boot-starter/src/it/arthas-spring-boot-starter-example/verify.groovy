def file = new File(basedir, "build.log")
return file.text.contains("Arthas agent start success.")

