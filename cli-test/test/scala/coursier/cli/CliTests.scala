package coursier.cli

import java.io.{File, FileWriter}

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class CliTests extends FlatSpec {

  def withFile(testCode: (File, FileWriter) => Any) {
    val file = File.createTempFile("hello", "world") // create the fixture
    val writer = new FileWriter(file)
    try {
      testCode(file, writer) // "loan" the fixture to the test
    }
    finally {
      writer.close()
      file.delete()
    }
  }

  // This test needs the file fixture
  "Normal text" should "parse correctly" in withFile { (file, writer) =>
    writer.write("org1:name1--org2:name2")
    writer.flush()

    val opt = CommonOptions(softExcludeFile = file.getAbsolutePath)
    val helper = new Helper(opt, Seq())
    assert(helper.softExcludeMap.equals(Map("org1:name1" -> Set(("org2", "name2")))))
  }

  // This test needs the file fixture
  "Multiple excludes" should "be combined" in withFile { (file, writer) =>
    writer.write("org1:name1--org2:name2\n" +
      "org1:name1--org3:name3\n" +
      "org4:name4--org5:name5")
    writer.flush()

    val opt = CommonOptions(softExcludeFile = file.getAbsolutePath)
    val helper = new Helper(opt, Seq())
    assert(helper.softExcludeMap.equals(Map(
      "org1:name1" -> Set(("org2", "name2"), ("org3", "name3")),
      "org4:name4" -> Set(("org5", "name5")))))
  }

  // This test needs the file fixture
  "extra --" should "error" in withFile { (file, writer) =>
    writer.write("org1:name1--org2:name2--xxx\n" +
      "org1:name1--org3:name3\n" +
      "org4:name4--org5:name5")
    writer.flush()
    assertThrows[SoftExcludeParsingException]({
      val opt = CommonOptions(softExcludeFile = file.getAbsolutePath)
      new Helper(opt, Seq())
    })
  }

  // This test needs the file fixture
  "child has no name" should "error" in withFile { (file, writer) =>
    writer.write("org1:name1--org2:")
    writer.flush()
    assertThrows[SoftExcludeParsingException]({
      val opt = CommonOptions(softExcludeFile = file.getAbsolutePath)
      new Helper(opt, Seq())
    })
  }

  // This test needs the file fixture
  "child has nothing" should "error" in withFile { (file, writer) =>
    writer.write("org1:name1--:")
    writer.flush()
    assertThrows[SoftExcludeParsingException]({
      val opt = CommonOptions(softExcludeFile = file.getAbsolutePath)
      new Helper(opt, Seq())
    })
  }

}
