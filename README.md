Partial Modification Loader(PML) is a mod/plugin system designed for generic Java application interfacing, utilizing elements of Aspect Oriented Program. (Uses Oracle Java 7 - Dalvik not supported)


This is the Agent code for PML - this portion of the code deals with using the Java Instrumentation API to transform/re-transform important classes concerning loading. This is separate from the PreMain code in order to support usage in standard jre installations.