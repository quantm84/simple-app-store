package sas.util

import sas.json.SASError

case class SASException(error: SASError) extends RuntimeException(s"code ${error.code}: ${error.message}")
