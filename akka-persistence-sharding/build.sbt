lazy val root = Project("root", file("."))
  .aggregate(server)
  .settings(BaseSettings.defaultSettings: _*)
  .disablePlugins(AssemblyPlugin)

lazy val server = Project("server", file("server"))
  .settings(BaseSettings.defaultSettings: _*)
  .settings(Dependencies.server: _*)
  .settings(Assembly.defaultSettings: _*)