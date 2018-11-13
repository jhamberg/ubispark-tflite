import java.io._
import scala.io.Source


/**
Usage: 
 scala CheckGroundTruth ILSVRC2012_validation_ground_truth.txt labels_all.txt results_file.txt
*/
object CheckGroundTruth{

  def main(args: Array[String]) {
   // row number -> row number of labels_all.txt
    val gtIdxToText = Source.fromFile(args(0)).getLines.toSeq.zipWithIndex.map{case (gt, idx) => (idx+1, gt.toInt)}.toMap
    
    // labels_all.txt: row number -> list of text descriptions
    val labels = Source.fromFile(args(1)).getLines.toSeq.zipWithIndex.map{case (labels, idx) => (idx+1, labels.split(", "))}.toMap
    
    val results = Source.fromFile(args(2)).getLines.toSeq.map{_.split(" ") match { 
      case Array(idtext, labeltext) => idtext.toInt -> labeltext
      }
    }

    var pmatches = 0.0    
    var matches = 0.0
    var resultcount = 0.0
    results.foreach{case (id, label) =>
      val gtTexts = labels.get(gtIdxToText.get(id).get).get
      println(s"GT ${gtTexts.mkString(", ")} result: ${label}")
      if (gtTexts.count(x => x == label) > 0)
        matches += 1
      if (gtTexts.count(x => x.contains(label) || label.contains(x)) > 0)
        pmatches += 1
      resultcount += 1
    }
    
    println(s"Results $resultcount matches $matches pmatches $pmatches accuracy ${matches/resultcount} ${pmatches/resultcount}")
  }
}
