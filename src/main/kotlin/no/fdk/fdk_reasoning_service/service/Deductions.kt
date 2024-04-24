package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule

fun CatalogType.deductionsModel(catalogData: Model, rdfData: ExternalRDFData): Model =
    when (this) {
        CatalogType.CONCEPTS -> ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(conceptRules)),
            catalogData
        ).deductionsModel

        CatalogType.DATASETS -> catalogData.fdkPrefix().datasetDeductions(rdfData)
        CatalogType.DATASERVICES -> ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(dataServiceRules)),
            catalogData
        ).deductionsModel

        CatalogType.INFORMATIONMODELS -> catalogData.informationModelDeductions(rdfData)
        CatalogType.PUBLICSERVICES -> catalogData.servicesDeductions(rdfData)
        else -> ModelFactory.createDefaultModel()
    }

private fun Model.informationModelDeductions(rdfData: ExternalRDFData): Model {
    val deductions = ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(infoModelRules)),
        this
    ).deductionsModel

    val themeLabelDeductions = union(deductions).themePrefLabelDeductions(rdfData)

    return deductions.union(themeLabelDeductions)
}

private fun Model.servicesDeductions(rdfData: ExternalRDFData): Model {
    val deductions = ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(serviceRules)),
        this
    ).deductionsModel

    val themeLabelDeductions = union(deductions).themePrefLabelDeductions(rdfData)

    return deductions.union(themeLabelDeductions)
}

private fun Model.datasetDeductions(rdfData: ExternalRDFData): Model {
    val deductions = ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(datasetRules)).bindSchema(rdfData.losData),
        this
    ).deductionsModel

    val themeLabelDeductions = union(deductions).themePrefLabelDeductions(rdfData)

    return deductions.union(themeLabelDeductions)
}

private fun Model.themePrefLabelDeductions(rdfData: ExternalRDFData): Model {
    // tag themes that's missing prefLabel in input model, to easier add all lang-options for relevant themes
    val themesMissingLabels = ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(tagThemesMissingLabel)),
        this
    ).deductionsModel

    // get theme labels as dct:title, so as not to confuse reasoner when adding them as prefLabel to input model later
    val themeTitles = ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(labelToTitle)),
        rdfData.losData.union(rdfData.eurovocs).union(rdfData.dataThemes)
    ).deductionsModel

    // add prefLabel from themeTitles for themes tagged as missing label
    return ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(addThemeTitles)).bindSchema(themeTitles.union(themesMissingLabels)),
        this
    ).deductionsModel
}

