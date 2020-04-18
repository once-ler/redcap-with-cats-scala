package com.eztier.redcap.client
package domain

import io.circe.generic.extras._

// @ConfiguredJsonCodec
case class Field
(
  BranchingLogic: Option[String] = None, // Any branching logic that REDCap uses to show or hide fields
  CustomAlignment: Option[String] = None, // (Web-only) Determines how the field looks visually
  FieldLabel: Option[String] = None, // The field label
  FieldName: Option[String] = None, // The raw field name
  FieldNote: Option[String] = None, // Any notes for this field
  FieldType: Option[String] = None, // The field type (text, radio, mult choice, etc.)
  FormName: Option[String] = None, // Form under which this field exists
  Identifier: Option[String] = None, // Whether this field has been marked as containing identifying information
  MatrixGroupName: Option[String] = None, // The matrix name this field belongs to
  QuestionNumber: Option[String] = None, // For survey fields, the survey number
  RequiredField: Option[String] = None, // Whether the field is required
  SelectChoicesOrCalculations: Option[String] = None, // For radio fields, the choices
  SectionHeader: Option[String] = None, // Under which section in the form page this field belongs
  TextValidationMin: Option[String] = None, // Minimum value for validation
  TextValidationMax: Option[String] = None, // Maximum value for validation
  TextValidationTypeOrShowSliderNumber: Option[String] = None // Validation type  
)
