/**
 * SMT-LIB (v2.6) grammar
 *
 * Grammar is baesd on the following specification:
 * http://smtlib.cs.uiowa.edu/papers/smt-lib-reference-v2.6-r2017-07-18.pdf
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Julian Thome <julian.thome.de@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 **/

grammar SMTLIBv2;

// Parser Rules Start

response
    : general_response_success
    | general_response_unsupported
    | general_response_error
    | specific_success_response
    ;

general_response_success
    : PS_Success
    ;

general_response_unsupported
    : PS_Unsupported
    ;

general_response_error
    : ParOpen PS_Error reason=String ParClose
    ;

specific_success_response
    : check_sat_response
    | get_unsat_core_response
    | get_model_response
    | get_interpolants_response_smtinterpol
    ;

check_sat_response
    : value=PS_Sat
    | value=PS_Unsat
    | value=PS_Unknown
    ;

get_unsat_core_response
    : ParOpen symbol* ParClose
    ;

get_model_response
    : ParOpen PS_Model? model_response* ParClose
    ;

model_response
    : model_response_fun
    | model_response_fun_rec
    | model_response_funs_rec
    | model_response_mathsat
    ;

model_response_fun
    : ParOpen CMD_DefineFun function_def ParClose
    ;

model_response_fun_rec
    : ParOpen CMD_DefineFunRec function_def ParClose
    ;

model_response_funs_rec
    : ParOpen CMD_DefineFunsRec ParOpen function_dec+ ParClose ParOpen term+ ParClose ParClose
    ;

model_response_mathsat
    : ParOpen symbol term ParClose
    ;

function_def
    : symbol ParOpen sorted_var* ParClose sort term
    ;

function_dec
    : ParOpen symbol ParOpen sorted_var* ParClose sort ParClose
    ;

get_interpolants_response_smtinterpol
    : ParOpen term* ParClose
    ;

// Parser Rules End

// Parser Rules Start

// Starting rule(s)

simpleSymbol
    : predefSymbol
    | UndefinedSymbol
    ;

quotedSymbol
    : QuotedSymbol
    ;

predefSymbol
    : PS_Not
    | PS_Bool
    | PS_ContinuedExecution
    | PS_Error
    | PS_False
    | PS_ImmediateExit
    | PS_Incomplete
    | PS_Logic
    | PS_Memout
    | PS_Sat
    | PS_Success
    | PS_Theory
    | PS_True
    | PS_Unknown
    | PS_Unsupported
    | PS_Unsat
    ;

predefKeyword
    : PK_AllStatistics
    | PK_AssertionStackLevels
    | PK_Authors
    | PK_Category
    | PK_Chainable
    | PK_Definition
    | PK_DiagnosticOutputChannel
    | PK_ErrorBehaviour
    | PK_Extension
    | PK_Funs
    | PK_FunsDescription
    | PK_GlobalDeclarations
    | PK_InteractiveMode
    | PK_Language
    | PK_LeftAssoc
    | PK_License
    | PK_Named
    | PK_Name
    | PK_Notes
    | PK_Pattern
    | PK_PrintSuccess
    | PK_ProduceAssertions
    | PK_ProduceAssignments
    | PK_ProduceModels
    | PK_ProduceProofs
    | PK_ProduceUnsatAssumptions
    | PK_ProduceUnsatCores
    | PK_RandomSeed
    | PK_ReasonUnknown
    | PK_RegularOutputChannel
    | PK_ReproducibleResourceLimit
    | PK_RightAssoc
    | PK_SmtLibVersion
    | PK_Sorts
    | PK_SortsDescription
    | PK_Source
    | PK_Status
    | PK_Theories
    | PK_Values
    | PK_Verbosity
    | PK_Version
    ;

symbol
    : simpleSymbol
    | quotedSymbol
    ;

numeral
    : Numeral
    ;

decimal
    : Decimal
    ;

hexadecimal
    : HexDecimal
    ;

binary
    : Binary
    ;

string
    : String
    ;

keyword
    : predefKeyword
    | Colon simpleSymbol
    ;

// S-expression

spec_constant
    : numeral
    | decimal
    | hexadecimal
    | binary
    | string
    ;


s_expr
    : spec_constant
    | symbol
    | keyword
    | ParOpen s_expr* ParClose
    ;

// Identifiers

index
    : numeral
    | symbol
    ;

identifier
    : symbol
    | ParOpen GRW_Underscore symbol index+ ParClose
    ;

// Attributes

attribute_value
    : spec_constant
    | symbol
    | ParOpen s_expr* ParClose
    ;

attribute
    : keyword
    | keyword attribute_value
    ;

// Sorts

sort
    : identifier
    | ParOpen identifier sort+ ParClose
    ;


// Terms and Formulas

qual_identifier
    : identifier
    | ParOpen GRW_As identifier sort ParClose
    ;

var_binding
    : ParOpen symbol term ParClose
    ;

sorted_var
    : ParOpen symbol sort ParClose
    ;

pattern
    : symbol
    | ParOpen symbol symbol+ ParClose
    ;

match_case
    : ParOpen pattern term ParClose
    ;

term
    : spec_constant
    | qual_identifier
    | generic_term
    | let_term
    | forall_term
    | exists_term
    | match_term
    | annotate_term
    ;

generic_term
    : ParOpen qual_identifier term+ ParClose
    ;

let_term
    : ParOpen GRW_Let ParOpen var_binding+ ParClose term ParClose
    ;

forall_term
    : ParOpen GRW_Forall ParOpen sorted_var+ ParClose term ParClose
    ;

exists_term
    : ParOpen GRW_Exists ParOpen sorted_var+ ParClose term ParClose
    ;

match_term
    : ParOpen GRW_Match term ParOpen match_case+ ParClose ParClose
    ;

annotate_term
    : ParOpen GRW_Exclamation term attribute+ ParClose
    ;

// Parser Rules End

// Lexer Rules Start

Comment
    : Semicolon ~[\r\n]* -> skip
    ;

ParOpen
    : '('
    ;

ParClose
    : ')'
    ;

Semicolon
    : ';'
    ;

String
    : '"' (PrintableCharNoDquote | WhiteSpaceChar)+ '"'
    ;

QuotedSymbol:
    '|' (PrintableCharNoBackslash | WhiteSpaceChar)+ '|'
    ;


// Predefined Symbols

PS_Not
    : 'not'
    ;
PS_Bool
    : 'Bool'
    ;
PS_ContinuedExecution
    : 'continued-execution'
    ;
PS_Error
    : 'error'
    ;
PS_False
    : 'false'
    ;
PS_ImmediateExit
    : 'immediate-exit'
    ;
PS_Incomplete
    : 'incomplete'
    ;
PS_Logic
    : 'logic'
    ;
PS_Memout
    : 'memout'
    ;
PS_Model
    : 'model'
    ;
PS_Sat
    : 'sat'
    ;
PS_Success
    : 'success'
    ;
PS_Theory
    : 'theory'
    ;
PS_True
    : 'true'
    ;
PS_Unknown
    : 'unknown'
    ;
PS_Unsupported
    : 'unsupported'
    ;
PS_Unsat
    : 'unsat'
    ;

// RESERVED Words

// Command names


CMD_Assert
    : 'assert'
    ;
CMD_CheckSat
    : 'check-sat'
    ;
CMD_CheckSatAssuming
    : 'check-sat-assuming'
    ;
CMD_DeclareConst
    : 'declare-const'
    ;
CMD_DeclareDatatype
    : 'declare-datatype'
    ;
CMD_DeclareDatatypes
    : 'declare-datatypes'
    ;
CMD_DeclareFun
    : 'declare-fun'
    ;
CMD_DeclareSort
    : 'declare-sort'
    ;
CMD_DefineFun
    : 'define-fun'
    ;
CMD_DefineFunRec
    : 'define-fun-rec'
    ;
CMD_DefineFunsRec
    : 'define-funs-rec'
    ;
CMD_DefineSort
    : 'define-sort'
    ;
CMD_Echo
    : 'echo'
    ;
CMD_Exit
    : 'exit'
    ;
CMD_GetAssertions
    : 'get-assertions'
    ;
CMD_GetAssignment
    : 'get-assignment'
    ;
CMD_GetInfo
    : 'get-info'
    ;
CMD_GetModel
    : 'get-model'
    ;
CMD_GetOption
    : 'get-option'
    ;
CMD_GetProof
    : 'get-proof'
    ;
CMD_GetUnsatAssumptions
    : 'get-unsat-assumptions'
    ;
CMD_GetUnsatCore
    : 'get-unsat-core'
    ;
CMD_GetValue
    : 'get-value'
    ;
CMD_Pop
    : 'pop'
    ;
CMD_Push
    : 'push'
    ;
CMD_Reset
    : 'reset'
    ;
CMD_ResetAssertions
    : 'reset-assertions'
    ;
CMD_SetInfo
    : 'set-info'
    ;
CMD_SetLogic
    : 'set-logic'
    ;
CMD_SetOption
    : 'set-option'
    ;




// General reserved words

GRW_Exclamation
    : '!'
    ;
GRW_Underscore
    : '_'
    ;
GRW_As
    : 'as'
    ;
GRW_Binary
    : 'BINARY'
    ;
GRW_Decimal
    : 'DECIMAL'
    ;
GRW_Exists
    : 'exists'
    ;
GRW_Hexadecimal
    : 'HEXADECIMAL'
    ;
GRW_Forall
    : 'forall'
    ;
GRW_Let
    : 'let'
    ;
GRW_Match
    : 'match'
    ;
GRW_Numeral
    : 'NUMERAL'
    ;
GRW_Par
    : 'par'
    ;
GRW_String
    : 'string'
    ;

Numeral
    : '0'
    | [1-9] Digit*
    ;

Binary
    : '#b' BinaryDigit+
    ;

HexDecimal
    : '#x' HexDigit+
    ;

Decimal
    : Numeral '.' '0'* Numeral
    ;



fragment HexDigit
    : '0' .. '9' | 'a' .. 'f' | 'A' .. 'F'
    ;


Colon
    : ':'
    ;

fragment Digit
    : [0-9]
    ;

fragment Sym
    : 'a'..'z'
    | 'A' .. 'Z'
    | '+'
    | '='
    | '/'
    | '*'
    | '%'
    | '?'
    | '!'
    | '$'
    | '-'
    | '_'
    | '~'
    | '&'
    | '^'
    | '<'
    | '>'
    | '@'
    | '.'
    ;



fragment BinaryDigit
    : [01]
    ;

fragment PrintableChar
    : '\u0020' .. '\u007E'
    | '\u0080' .. '\uffff'
    | EscapedSpace
    ;

fragment PrintableCharNoDquote
    : '\u0020' .. '\u0021'
    | '\u0023' .. '\u007E'
    | '\u0080' .. '\uffff'
    | EscapedSpace
    ;

fragment PrintableCharNoBackslash
    : '\u0020' .. '\u005B'
    | '\u005D' .. '\u007B'
    | '\u007D' .. '\u007E'
    | '\u0080' .. '\uffff'
    | EscapedSpace
    ;

fragment EscapedSpace
    : '""'
    ;

fragment WhiteSpaceChar
    : '\u0009'
    | '\u000A'
    | '\u000D'
    | '\u0020'
    ;

// Lexer Rules End

// Predefined Keywords



PK_AllStatistics
    : ':all-statistics'
    ;
PK_AssertionStackLevels
    : ':assertion-stack-levels'
    ;
PK_Authors
    : ':authors'
    ;
PK_Category
    : ':category'
    ;
PK_Chainable
    : ':chainable'
    ;
PK_Definition
    : ':definition'
    ;
PK_DiagnosticOutputChannel
    : ':diagnostic-output-channel'
    ;
PK_ErrorBehaviour
    : ':error-behavior'
    ;
PK_Extension
    : ':extensions'
    ;
PK_Funs
    : ':funs'
    ;
PK_FunsDescription
    : ':funs-description'
    ;
PK_GlobalDeclarations
    : ':global-declarations'
    ;
PK_InteractiveMode
    : ':interactive-mode'
    ;
PK_Language
    : ':language'
    ;
PK_LeftAssoc
    : ':left-assoc'
    ;
PK_License
    : ':license'
    ;
PK_Named
    : ':named'
    ;
PK_Name
    : ':name'
    ;
PK_Notes
    : ':notes'
    ;
PK_Pattern
    : ':pattern'
    ;
PK_PrintSuccess
    : ':print-success'
    ;
PK_ProduceAssertions
    : ':produce-assertions'
    ;
PK_ProduceAssignments
    : ':produce-assignments'
    ;
PK_ProduceModels
    : ':produce-models'
    ;
PK_ProduceProofs
    : ':produce-proofs'
    ;
PK_ProduceUnsatAssumptions
    : ':produce-unsat-assumptions'
    ;
PK_ProduceUnsatCores
    : ':produce-unsat-cores'
    ;
PK_RandomSeed
    : ':random-seed'
    ;
PK_ReasonUnknown
    : ':reason-unknown'
    ;
PK_RegularOutputChannel
    : ':regular-output-channel'
    ;
PK_ReproducibleResourceLimit
    : ':reproducible-resource-limit'
    ;
PK_RightAssoc
    : ':right-assoc'
    ;
PK_SmtLibVersion
    : ':smt-lib-version'
    ;
PK_Sorts
    : ':sorts'
    ;
PK_SortsDescription
    : ':sorts-description'
    ;
PK_Source
    : ':source'
    ;
PK_Status
    : ':status'
    ;
PK_Theories
    : ':theories'
    ;
PK_Values
    : ':values'
    ;
PK_Verbosity
    : ':verbosity'
    ;
PK_Version
    : ':version'
    ;

UndefinedSymbol:
    Sym (Digit | Sym)*;

WS  :  [ \t\r\n]+ -> skip
    ;