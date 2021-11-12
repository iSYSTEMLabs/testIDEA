@echo off
rem This file generates classes in the packsge si.isystem.cte.model
rem from XML schema provided by B&M.
rem Run this script only when new scema is provided. Since it
rem is not expected to ocurr often, the generated files are under
rem version control, which also simplifies the build procedure -
rem no need to run this script for rarely changing XML schema.

rem MK, 28.9.2012
@echo on

xjc -p si.isystem.cte.model -d src resources/cte.xsd
