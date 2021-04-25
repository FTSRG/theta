# LLVM Instruction mapping

These tables detail the currently supported LLVM instructions and their corresponding model elements. Note, that a
dash (`-`) denotes an instruction that is recognized but _won't_ cause any alteration to the model (because it is
out-of-scope), while a `TODO` label means that it currently produces an error to use the instruction, but should be
handled accordingly.

## Terminator Instructions

| LLVM Instructions | Handled versions | Resulting model element(s)|
| --- | --- | --- |
|`ret` | 1. `ret`<br>2. `ret <expr>` | 1. Edge to final location<br>2. Assignment to return variable, edge to final location
|`br` | 1. `br <label>`<br>2. `br <expr> <label> <label>` | 1. Edge to first location of labelled block<br>2. Edge to first location of first labelled block with a guard `<expr>`, edge to first location of second labelled block with a guard `!<expr>`
|`switch` | `switch <var> <label> [<const> <label>]*` | Edge to first location of each labelled block with a guard `<var>==<const>`, and the default edge with a guard `!(OR(<var>==<const>))`
|`indirectbr` | - | -
|`invoke` | - | -
|`callbr` | - | -
|`resume` | - | -
|`catchswitch` | - | -
|`catchret` | - | -
|`cleanupret` | - | -
|`unreachable` | `unreachable` | Nothing

## Unary Operations

| LLVM Instructions | Handled versions | Resulting model element(s)|
| --- | --- | --- |
|`fneg` | TODO | TODO

## Binary Operations

| LLVM Instructions | Handled versions | Resulting model element(s)|
| --- | --- | --- |
|`add` | `<reg> = add <expr> <expr>` | `<reg> = <expr> + <expr>`
|`fadd` | TODO | TODO
|`sub` | `<reg> = sub <expr> <expr>` | `<reg> = <expr> - <expr>`
|`fsub` | TODO | TODO
|`mul` |  `<reg> = mul <expr> <expr>` | `<reg> = <expr> * <expr>`
|`fmul` | TODO | TODO
|`udiv` | `<reg> = udiv <expr> <expr>` | `<reg> = <expr> / <expr>`
|`sdiv` | `<reg> = sdiv <expr> <expr>` | `<reg> = <expr> / <expr>`
|`fdiv` | TODO | TODO
|`urem` | `<reg> = urem <expr> <expr>` | `<reg> = <expr> % <expr>`
|`srem` | `<reg> = urem <expr> <expr>` | `<reg> = <expr> % <expr>`
|`frem` | TODO | TODO

## Bitwise Binary Operations

Note: bitwise instructions are not yet implemented for >1 bit types. | LLVM Instructions | Handled versions | Resulting
model element(s)| | --- | --- | --- | |`shl` | TODO | TODO |`lshr` | TODO | TODO |`ashr` | TODO | TODO |`and`
|`<reg> = and <expr> <expr>` | `<reg> = <expr> & <expr>`
|`or` | `<reg> = or <expr> <expr>` | `<reg> = <expr> \| <expr>`
|`xor` |`<reg> = xor <expr> <expr>` | `<reg> = <expr> ^ <expr>`

## Vector Operations

| LLVM Instructions | Handled versions | Resulting model element(s)|
| --- | --- | --- |
|`extractelement` | TODO | TODO
|`insertelement` | TODO | TODO
|`shufflevector` | - | -

## Aggregate Operations

| LLVM Instructions | Handled versions | Resulting model element(s)|
| --- | --- | --- |
|`extractvalue` | TODO | TODO
|`insertvalue` | TODO | TODO

## Memory Access and Addressing Operations

| LLVM Instructions | Handled versions | Resulting model element(s)|
| --- | --- | --- |
|`alloca` | `<reg> = alloca` | New local variable
|`load` | `<reg> = load <reg>` | LHS reg will be referencing the RHS reg
|`store` | `store <reg> <expr>` | The variable referenced by `<reg>` will be assigned `<expr>`
|`fence` | - | -
|`cmpxchg` | - | -
|`atomicrmw` | - | -
|`getelementptr` | TODO | TODO

## Conversion Operations

| LLVM Instructions | Handled versions | Resulting model element(s)|
| --- | --- | --- |
|`trunc .. to` | `trunc .. to` | Akin to `load`
|`zext .. to` | `zext .. to` | Akin to `load`
|`sext .. to` | `sext .. to` | Akin to `load`
|`fptrunc .. to` | TODO | TODO
|`fpext .. to` | TODO | TODO
|`fptoui .. to` | TODO | TODO
|`fptosi .. to` | TODO | TODO
|`uitofp .. to` | TODO | TODO
|`sitofp .. to` | TODO | TODO
|`ptrtoint .. to` | - | -
|`inttoptr .. to` | - | -
|`bitcast .. to` | `bitcast .. to` | Akin to `load`
|`addrspacecast .. to` | - | -

## Other Operations

| LLVM Instructions | Handled versions | Resulting model element(s)|
| --- | --- | --- |
|`icmp` | `<reg> = icmp <pred> <expr> <expr>` | Based on `<pred>`, `<reg>` will reference a boolean expression (Eq, Neq, Gt, Geq, Lt, Leq)
|`fcmp` | TODO | TODO
|`phi` | `<reg> = phi [<expr> <label>]*` | A new variable will be created, and from each labelled block the edge going to the current block will assing it the corresponding `<expr>`
|`select` | `<reg> = select <cond> <expr> <expr>` | `<reg> = <cond> ? <expr> : <expr>`
|`freeze` | - | -
|`call` | 1. `call [<expr>]* <funcname>`<br>2.  `<reg> = call [<expr>]* <funcname>`  | A new edge with a CallStmt will be added, calling `<funcname>` with params from `[<expr>]*` if `<funcname>` is locally available (defined). Otherwise, handle as intrinsic function call. When all fails, `havoc` each dereferenced `<reg>` and variable, as well as the return value when it exists.
|`va_arg` | - | -
|`landingpad` | - | -
|`catchpad` | - | -
|`cleanuppad` | - | -
