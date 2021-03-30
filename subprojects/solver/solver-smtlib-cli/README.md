## Overview

The `solver-smtlib-cli` project is an executable (command line) tool for managing the SMT-LIB compatible solvers.
For more information about the SMT-LIB compatibility in Theta, take a look at the [`solver-smtlib`](../solver-smtlib/README.md) project.

### Related projects

* [`solver`](../solver/README.md): Contains the generic utilities and solver interface of Theta.
* [`solver-smtlib`](../solver-smtlib/README.md): Implements Theta's solver interface to support SMT-LIB compatible solvers.

## Using the tool

1. First, get the tool.
    * The easiest way is to download a [pre-built release](https://github.com/ftsrg/theta/releases).
    * You can also [build](../../../doc/Build.md) the tool yourself. The runnable jar file will appear under _build/libs/_ with the name _theta-solver-smtlib-cli-\<VERSION\>-all.jar_, you can simply rename it to _theta-solver-smtlib-cli.jar_..
2. Running the tool requires Java (JRE) 11.
3. The tool can be executed with `java -jar theta-solver-smtlib-cli.jar [MAIN ARGUMENTS] [COMMAND] [ARGUMENTS]`.
    * If no arguments are given, a help screen is displayed about the arguments and their possible values.
    More information can also be found below.

## Arguments

The tool supports the following main arguments. These are all optional:
* `--home`: Sets the path of the solver registry. It defaults to a folder named _.theta_ in the home folder of the current user.
* `--help`: Prints the help message.

The tool supports the following commands and their arguments:

* `install <solver_name>:<solver_version>`: Installs a solver with the given name and version to the current solver registry. The _<solver_name>_ identifies the driver (a.k.a. the type) of the solver, while the _<solver_version>_ identifies the version of the solver to install. (See the list of supported solvers and their versions with `list-supported`)
    * `--name`: Install the solver version under this custom name (<solver_name>:<name>), instead of the default (<solver_name>:<solver_version>)
    * `--tempt-murphy`: Enables the installation of unsupported solver versions. If you enable this, you can expect things to break, as these solvers were not tested with Theta at all!
* `install-generic`: Installs an SMT-LIB compatible solver with the generic driver. For more information see the [Generic driver](#Generic-driver) section.
    * `--solver-path` **[required]**: Denotes the path to the binary of the solver.
    * `--solver-args`: The command line arguments to pass to the generic solver.
    * `--name` **[required]**: Install the solver under this custom name (generic:<name>)
* `uninstall <solver_name>:<solver_version>`: Uninstalls a solver with the given name and version from the current solver registry. (See the list of installed solvers and their versions with `list-installed`)
* `reinstall <solver_name>:<solver_version>`: Reinstalls a solver with the given name and version in the current solver registry. (See the list of installed solvers and their versions with `list-installed`.) **Note**: This command is not available for solvers installed with the generic driver.
* `get-info <solver_name>:<solver_version>`: Gets the stored information about the solver with the given name and version in the current solver registry. (See the list of installed solvers and their versions with `list-installed`)
* `edit-args <solver_name>:<solver_version>`: Edits the command line arguments of the solver with the given name and version. The command opens a txt file that stores the command line arguments to edit with the default editor, or prints the path of the file to edit if the default editor is not applicable (e.g. CLI). (See the list of installed solvers and their versions with `list-installed`) 
    * `--print`: If given, the path of the file to edit will be printed instead of opening.
* `list-installed [<solver_name>]`: Lists the installed solvers and their versions. If the _<solver_name>_ is given, only the versions of that solver will be listed.
* `list-supported [<solver_name>]`: Lists the supported solvers and their versions. If the _<solver_name>_ is given, only the versions of that solver will be listed.

### For developer usage

The following main arguments are targeted for developers:

| Flag | Description |
|---|---|
| `--stacktrace` | Print full stack trace for exceptions. |
| `--loglevel` | `--loglevel`: Detailedness of logging. Possible values (from the least to the most detailed): `RESULT`, `MAINSTEP`, `SUBSTEP` (default), `INFO`, `DETAIL`, `VERBOSE` |

## Generic driver

It is possible to integrate any fully SMT-LIB compatible solvers with Theta using the generic driver. To do this, the following information is needed:

* The path to the binary of the solver. If the solver does not have a binary (e.g. it is Java based), then a bash script should be created that serves as an entry point for the solver.
* The command line arguments to pass to the solver. These arguments should configure the solver for the following behavior:
    * It must read its input from the standard input.
    * It must write its output to the standard output. Moreover, it should flush the standard output after every line.
    * It **must not** write anything else to the standard output that is not part of the SMT-LIB standard. The first command the driver issues is the `(set-option :print-success true)`, so the first characters the binary outputs should be the result of this command!
    
For more information see the default configurations of the supported solvers.

## Supported solvers

Currently, the following solvers are supported.

* Z3
    * `4.4.0` - `4.6.0`: Supported with interpolation.
    * `4.7.1` - `4.8.9`: Supported without interpolation. Interpolation was removed in `4.7.0`.
* CVC4
    * `1.0` - `1.8`: Supported.