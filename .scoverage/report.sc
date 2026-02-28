//> using scala 3.8.2
//> using dep org.scoverage::scalac-scoverage-reporter:2.5.2
//> using dep org.scoverage::scalac-scoverage-domain:2.5.2
//> using dep org.scoverage::scalac-scoverage-serializer:2.5.2

import scoverage.reporter.ScoverageHtmlWriter
import scoverage.reporter.CoberturaXmlWriter
import scoverage.reporter.IOUtils
import java.io.File
import scoverage.serialize.Serializer

val coverageFile = new File(".scoverage/scoverage.coverage")
val sourceDir = new File(".")
val measurementDir = new File(".scoverage")
val outDir = new File(".scoverage/report")

if (!coverageFile.exists()) {
  println(s"Error: Coverage file not found at ${coverageFile.getAbsolutePath}")
  sys.exit(1)
}

val coverage = Serializer.deserialize(coverageFile, sourceDir)

val measurementFiles = IOUtils.findMeasurementFiles(measurementDir)

val measurements = IOUtils.invoked(measurementFiles.toIndexedSeq)
coverage.apply(measurements)

outDir.mkdirs()

val htmlReporter = new ScoverageHtmlWriter(Seq(sourceDir), outDir, None)
htmlReporter.write(coverage)

val xmlReporter = new CoberturaXmlWriter(Seq(sourceDir), outDir, None)
xmlReporter.write(coverage)

println(s"Statement coverage: ${coverage.statementCoverageFormatted}%")
