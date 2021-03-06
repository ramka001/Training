if (issue.getIssueTypeId() != "10200") { return }

def statusChanged = event?.getChangeLog()?.getRelated("ChildChangeItem").find { it.field == "status" }
if (statusChanged.oldstring != "ODUSTAO") { return }

def zaPlatitiChanged = event?.getChangeLog()?.getRelated("ChildChangeItem").find { it.field.contains("Za platiti: redovna obuka") }
if (!zaPlatitiChanged) { return }

import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.workflow.WorkflowTransitionUtil
import com.atlassian.jira.workflow.WorkflowTransitionUtilImpl
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.user.ApplicationUser
import java.sql.Timestamp
import java.util.Date
import java.text.SimpleDateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import org.apache.log4j.Logger
import org.apache.log4j.Level
 
def log = Logger.getLogger("com.acme.ReaktiviranjeKandidataListener")
log.setLevel(Level.DEBUG)

DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ITALY);
f = new DecimalFormat("#,##0.00", symbols)
def issueManager = ComponentAccessor.getIssueManager()
def changeHolder = new DefaultIssueChangeHolder()
def cfManager = ComponentAccessor.getCustomFieldManager()
def issueFactory = ComponentAccessor.getIssueFactory()
def constantManager = ComponentAccessor.getConstantsManager()
def projectManager = ComponentAccessor.getProjectManager()
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getUser()
def subTaskManager = ComponentAccessor.getSubTaskManager()
ApplicationUser visol = ComponentAccessor.getUserManager().getUserByKey("VISOL")
def KAN = projectManager.getProjectObjects().find {it.getKey() == "KAN"}

def imeField = cfManager.getCustomFieldObject("customfield_10132")
def prezimeField = cfManager.getCustomFieldObject("customfield_10133")
def telefonField = cfManager.getCustomFieldObject("customfield_10220")
def viberField = cfManager.getCustomFieldObject("customfield_10221")
def emailField = cfManager.getCustomFieldObject("customfield_10222")
def ukupnoUplateField = cfManager.getCustomFieldObject("customfield_10806")
def prekoracenIznosField = cfManager.getCustomFieldObject("customfield_10819")
def zaPlatitiRedovnaField = cfManager.getCustomFieldObject("customfield_11256")
def zaPlatitiDodDopField = cfManager.getCustomFieldObject("customfield_11303")
def zaPlatitiUkupnoField = cfManager.getCustomFieldObject("customfield_11302")
def iznosRateField = cfManager.getCustomFieldObject("customfield_10801") 
def preostaloField = cfManager.getCustomFieldObject("customfield_10800") 
def datumDospjecaField = cfManager.getCustomFieldObject("customfield_10802")
def brojRataField = cfManager.getCustomFieldObject("customfield_10639")
def zavodniBrojField = cfManager.getCustomFieldObject("customfield_10321")
def uplacenaTeorijaField = cfManager.getCustomFieldObject("customfield_10637")
def cijenaTeorijeField = cfManager.getCustomFieldObject("customfield_10503")
def vrstaLicneField = cfManager.getCustomFieldObject("customfield_10200")
def brojLicneField = cfManager.getCustomFieldObject("customfield_10216")
def izdataUField = cfManager.getCustomFieldObject("customfield_10121")
def ocevoImeField = cfManager.getCustomFieldObject("customfield_10117")
def kategorijaField = cfManager.getCustomFieldObject("customfield_10203")
def datumLjekarskogField = cfManager.getCustomFieldObject("customfield_10212")
def brojLjekarskogField = cfManager.getCustomFieldObject("customfield_10215")
def brojPolaganjaField = cfManager.getCustomFieldObject("customfield_10627")
def datumUplateField = cfManager.getCustomFieldObject("customfield_10803")
def iznosUplateField = cfManager.getCustomFieldObject("customfield_10424")

// Za reporte
def ime = issue.getCustomFieldValue(imeField)
def prezime = issue.getCustomFieldValue(prezimeField)
def telefon = issue.getCustomFieldValue(telefonField)
def viber = issue.getCustomFieldValue(viberField)
def email = issue.getCustomFieldValue(emailField)
def zavodniBroj = issue.getCustomFieldValue(zavodniBrojField)
def cijenaTeorije = (issue.getCustomFieldValue(cijenaTeorijeField)) ? issue.getCustomFieldValue(cijenaTeorijeField).toInteger() : (double) 0.00
def vrstaLicne = issue.getCustomFieldValue(vrstaLicneField)
def brojLicne = issue.getCustomFieldValue(brojLicneField)
def izdataU = issue.getCustomFieldValue(izdataUField)
def ocevoIme = issue.getCustomFieldValue(ocevoImeField)
def kategorija = issue.getCustomFieldValue(kategorijaField)
def datumLjekarskog = issue.getCustomFieldValue(datumLjekarskogField)
def brojLjekarskog = issue.getCustomFieldValue(brojLjekarskogField)
def brojPolaganja = issue.getCustomFieldValue(brojPolaganjaField)

def zaPLatitiNew = zaPlatitiChanged.newstring.toDouble()
def zaPLatitiOld = zaPlatitiChanged.oldstring.toDouble()
def razlika = zaPLatitiNew - zaPLatitiOld
def ukupnoUplate =  (issue.getCustomFieldValue(ukupnoUplateField)) ? issue.getCustomFieldValue(ukupnoUplateField) : (double) 0
def iznosUplate = (issue.getCustomFieldValue(iznosUplateField)) ? issue.getCustomFieldValue(iznosUplateField) : (double)0 

def preostalo
def prekoracenIznos

if (zaPLatitiNew - ukupnoUplate - iznosUplate > 0) {
	preostalo = zaPLatitiNew - ukupnoUplate - iznosUplate
	prekoracenIznos = (double)0
} else {
	preostalo = (double)0
	prekoracenIznos = ukupnoUplate + iznosUplate - zaPLatitiNew
}

prekoracenIznosField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(prekoracenIznosField), prekoracenIznos), changeHolder)
preostaloField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(preostaloField), preostalo), changeHolder)

log.debug("ukupnoUplate: " + ukupnoUplate.toString())
log.debug("prekoracenIznos: " + prekoracenIznos.toString())
log.debug("preostalo: " + preostalo.toString())
log.debug("iznosUplate: " + iznosUplate.toString())

if (razlika == 0) { return }

SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
def todayStr = new Date().format("yyyy-MM-dd")
Date date = sdf.parse(incrementMonth(todayStr))
Timestamp datumDospjecaUplateVal = new Timestamp(date.getTime())
String datestr = date.format("dd.MM.yyyy")

def iznosRateString = f.format(razlika)
def summary 
if (razlika < 0) {
	summary = "Rata pla\u0107anja | promjena cijene | $ime $prezime | $datestr | \u20ac $iznosRateString"
} else {
	def uplacenoRata
	if (preostalo == 0) {
		uplacenoRata = razlika
	} else if (preostalo > razlika) {
		uplacenoRata = (double)0
	} else {
		uplacenoRata = razlika - preostalo
	}
	def preostaloRata = razlika - uplacenoRata
	def uplacenoString = f.format((uplacenoRata) ?: 0)
	summary = "Rata pla\u0107anja | promjena cijene | $ime $prezime | $datestr | \u20ac $uplacenoString/$iznosRateString"
}

MutableIssue rataIssue = issueFactory.getIssue()
rataIssue.setSummary(summary)
rataIssue.setParentObject(issue)
rataIssue.setProjectObject(KAN)
rataIssue.setIssueTypeId("10303")
rataIssue.setCustomFieldValue(imeField, ime)
rataIssue.setCustomFieldValue(prezimeField, prezime)
rataIssue.setCustomFieldValue(telefonField, telefon)
rataIssue.setCustomFieldValue(viberField, viber)
rataIssue.setCustomFieldValue(emailField, email)
rataIssue.setCustomFieldValue(iznosRateField, razlika)
if (razlika > 0) {
	rataIssue.setCustomFieldValue(preostaloField, razlika)	
}
rataIssue.setCustomFieldValue(zavodniBrojField, zavodniBroj)
rataIssue.setCustomFieldValue(datumDospjecaField, datumDospjecaUplateVal)
rataIssue.setCustomFieldValue(vrstaLicneField, vrstaLicne)
rataIssue.setCustomFieldValue(brojLicneField, brojLicne)
rataIssue.setCustomFieldValue(izdataUField, izdataU)
rataIssue.setCustomFieldValue(ocevoImeField, ocevoIme)
rataIssue.setCustomFieldValue(kategorijaField, kategorija)
rataIssue.setCustomFieldValue(datumLjekarskogField, datumLjekarskog)
rataIssue.setCustomFieldValue(brojLjekarskogField, brojLjekarskog)
rataIssue.setCustomFieldValue(brojPolaganjaField, brojPolaganja)
Map<String,Object> rataIssueParams = ["issue" : rataIssue] as Map<String,Object>
issueManager.createIssueObject(currentUser, rataIssueParams)
subTaskManager.createSubTaskIssueLink(issue, rataIssue, currentUser)
 
// Reset datum uplate, iznos uplate  
datumUplateField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(datumUplateField), null),changeHolder) 
iznosUplateField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(iznosUplateField), null),changeHolder)

// Poravnavanje rata sa razlikom
if (razlika < 0) {
	WorkflowTransitionUtil workflowTransitionUtil = ( WorkflowTransitionUtil ) JiraUtils.loadComponent( WorkflowTransitionUtilImpl.class );
	workflowTransitionUtil.setUserkey("visol")
	workflowTransitionUtil.setAction(31)
	workflowTransitionUtil.setIssue(rataIssue)
	workflowTransitionUtil.validate()
	workflowTransitionUtil.progress()
}

if (iznosUplate > 0) {
	WorkflowTransitionUtil workflowTransitionUtil = ( WorkflowTransitionUtil ) JiraUtils.loadComponent( WorkflowTransitionUtilImpl.class );
	workflowTransitionUtil.setUserkey("visol")

	def subtasks = issue.getSubTaskObjects().findAll { it.getStatusId() != "10403" }
	subtasks.sort{a, b -> a.getCustomFieldValue(datumDospjecaField) <=> b.getCustomFieldValue(datumDospjecaField) }

	subtasks.each { rata ->
		MutableIssue ratoza = rata
		if (iznosUplate > 0) {
			def datumDospjecaUplate = ratoza.getCustomFieldValue(datumDospjecaField)
			def datumstr = new Date(datumDospjecaUplate.getTime()).format("dd.MM.yyyy")
			def uplacenoNaRati = (ratoza.getCustomFieldValue(iznosUplateField)) ? ratoza.getCustomFieldValue(iznosUplateField) : (double)0 
			def novouplaceno = uplacenoNaRati + iznosUplate
			def iznosRate = (ratoza.getCustomFieldValue(iznosRateField)) ? ratoza.getCustomFieldValue(iznosRateField) : (double)0
			def sum = ratoza.summary
			sum = sum.substring(0, sum.size() - sum.split("\u20ac")[-1].size() + 1)
			if (iznosRate > novouplaceno) {
				sum = sum + f.format(novouplaceno) + "/" + f.format(iznosRate)
				ratoza.setSummary(sum)
				issueManager.updateIssue(visol, ratoza, EventDispatchOption.ISSUE_UPDATED, false)
				iznosUplateField.updateValue(null, ratoza, new ModifiedValue(ratoza.getCustomFieldValue(iznosUplateField), uplacenoNaRati + iznosUplate),changeHolder)
				preostaloField.updateValue(null, ratoza, new ModifiedValue(ratoza.getCustomFieldValue(preostaloField), iznosRate - uplacenoNaRati - iznosUplate),changeHolder)
				iznosUplate = 0
			} else {
				iznosUplate = iznosUplate - iznosRate + uplacenoNaRati
				sum = sum + f.format(iznosRate) + "/" + f.format(iznosRate)
				ratoza.setSummary(sum)
				issueManager.updateIssue(visol, ratoza, EventDispatchOption.ISSUE_UPDATED, false)
				iznosUplateField.updateValue(null, ratoza, new ModifiedValue(ratoza.getCustomFieldValue(iznosUplateField), iznosRate),changeHolder)
				preostaloField.updateValue(null, ratoza, new ModifiedValue(ratoza.getCustomFieldValue(preostaloField), (double)0),changeHolder)
				if (ratoza.getStatusId() == "10102") {  // Otvoreno
					workflowTransitionUtil.setAction (31)
	 				workflowTransitionUtil.setIssue(ratoza);
	    			workflowTransitionUtil.validate();
	    			workflowTransitionUtil.progress();
				} else if (ratoza.getStatusId() == "10402") {
					workflowTransitionUtil.setAction (21)
	 				workflowTransitionUtil.setIssue(ratoza);
	    			workflowTransitionUtil.validate();
	    			workflowTransitionUtil.progress();
				}
			}	
		}	 
	}
}


def incrementMonth(String date) {
	year = date.split("-")[0]
	month = date.split("-")[1]
	day = date.split("-")[2]

	if (month == "12") {
		month = "01"
		year = (year.toInteger() + 1).toString()
	} else {
		month = String.format("%02d", (month.toInteger() + 1))
	}

	if (day == "31") {
		if (month == "04" || month == "06" || month == "09" || month == "11") {
			day = "30"
		} 
	}

	if (month == "02" && day.toInteger() > 28) {
		if (isPrestupna()) {
			day = "29"
		} else {
			day = "28"
		}
	}
	return year + "-" + month + "-" + day
}


/*
def prekoracenIznos = (issue.getCustomFieldValue(prekoracenIznosField)) ? issue.getCustomFieldValue(prekoracenIznosField) : (double) 0 
def preostalo = (issue.getCustomFieldValue(preostaloField)) ? issue.getCustomFieldValue(preostaloField) : (double)0

if (razlika > 0) {
	log.debug("prekoraceni: " + prekoracenIznos.toString())
	if (prekoracenIznos > 0) {
		if (prekoracenIznos + iznosUplate > razlika) {
			prekoracenIznos = prekoracenIznos + iznosUplate - razlika
		} else {
			prekoracenIznos = (double)0
			preostalo = razlika - prekoracenIznos - iznosUplate
		}
		log.debug("preostalo: (prekoraceni > 0): " + preostalo.toString())
		log.debug("noviPrekoraceni: " + prekoracenIznos.toString())
	} else {
		if (iznosUplate > razlika + preostalo) {
			preostalo = (double)0
			prekoracenIznos = iznosUplate - razlika - preostalo
		} else {
			preostalo = preostalo + razlika - iznosUplate
		}
		log.debug("preostalo: (prekoraceni <= 0): " + preostalo.toString())
		log.debug("noviPrekoraceni: " + prekoracenIznos.toString())
	}
} else {
	log.debug("razlika < 0")
	if (preostalo - iznosUplate > razlika.abs()) {
		preostalo = preostalo - iznosUplate + razlika 
	} else {
		prekoracenIznos = iznosUplate + razlika.abs() - preostalo
		preostalo = (double) 0
	}
	log.debug("noviPrekoraceni: " + prekoracenIznos.toString())
	log.debug("preostalo: " + preostalo.toString())
prekoracenIznosField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(prekoracenIznosField), prekoracenIznos), changeHolder)
preostaloField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(preostaloField), preostalo), changeHolder)
}
*/