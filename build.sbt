name := "rp-with-monix"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "io.monix"       %% "monix"            % "2.2.3",

  "org.log4s"      %% "log4s"            % "1.3.4",
  "ch.qos.logback"  % "logback-classic"  % "1.1.7",

  "org.twitter4j"   % "twitter4j-stream" % "4.0.6"
)
    