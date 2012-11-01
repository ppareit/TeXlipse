package net.sourceforge.texlipse.builder.factory;

import net.sourceforge.texlipse.builder.ProgramRunner;

/**
 * Descriptive information and parameters which is used by the BuilderFactory to
 * instantiate a runner. Not all fields necessarily need to be provided.
 * <p>
 * This class also provides constants for XML tags.
 *
 * @author Matthias Erll
 */
public class RunnerDescription {

    public static final String RUNNER_XML_ROOT = "runners";
    public static final String RUNNER_XML_ELEMENT = "runner";
    public static final String RUNNER_XML_ID_ATTR = "id";

    // runner XML field elements
    public static final String[] RUNNER_PROPERTY_STR = { "label", "description",
            "inputFormat", "outputFormat", "runnerClass", "legacyClass",
            "isCoreRunner", "executable", "executable.windows", "defaultArguments" };

    // runner XML field identifiers
    public static enum RunnerProperty { LABEL, DESCRIPTION, INPUTFORMAT,
            OUTPUTFORMAT, RUNNERCLASS, LEGACYCLASS, ISCORERUNNER, EXECUTABLE,
            EXECUTABLE_WINDOWS, DEFAULTARGUMENTS };

    private final String id;
    private String label;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private Class<? extends ProgramRunner> runnerClass;
    private String legacyClass;
    private boolean coreRunner;
    private String executable;
    private String defaultArguments;

    /**
     * Retrieves the RunnerProperty object for the given XML tag name.
     *
     * @param propertyStr XML tag name
     * @return RunnerProperty, or <code>null</code> if none matches
     */
    public static RunnerProperty getRunnerProperty(final String propertyStr) {
        for (int i = 0; i < RUNNER_PROPERTY_STR.length; i++) {
            if (RUNNER_PROPERTY_STR[i].equals(propertyStr)) {
                return RunnerProperty.values()[i];
            }
        }
        return null;
    }

    /**
     * Retrieves the XML element tag name for the given RunnerProperty object.
     *
     * @param property RunnerProperty
     * @return XML tag name
     */
    public static String getXmlPropertyStr(final RunnerProperty property) {
        return RUNNER_PROPERTY_STR[property.ordinal()];
    }

    /**
     * Constructor for a new runner description.
     *
     * @param id unique id for the runner
     */
    public RunnerDescription(String id) {
        super();
        this.id = id;
    }

    /**
     * Returns the runner id.
     *
     * @return the runner id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the runner label. Should be, but does not have to be unique for
     * functioning properly.
     *
     * @return the runner label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the runner label.
     *
     * @param label the runner label
     */
    protected void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the short runner description, which is shown in the runner configuration.
     *
     * @return the runner description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the runner description.
     *
     * @param description the runner description
     */
    protected void setDescription(String description) {
        this.description = description;
    }

    /**
     * Retrieves the input format (file extension) of this program runner. This
     * is used by the BuilderRegistry to find appropriate runners for the builders.
     *
     * @return the input format, or <code>null</code> if not set
     */
    public String getInputFormat() {
        return inputFormat;
    }

    /**
     * Sets the input format.
     *
     * @param inputFormat the input format
     */
    protected void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    /**
     * Retrieves the output format (file extension), which is produced by this
     * program runner.
     *
     * @return output format, or <code>null</code> if not specified
     */
    public String getOutputFormat() {
        return outputFormat;
    }

    /**
     * Sets the output format for this runner.
     *
     * @param outputFormat the output format
     */
    protected void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * Returns the runner class, which the BuilderFactory uses for instantiating the
     * runner. This can be used for multiple runners, if only certain parameters
     * (e.g. the builder program) differ.
     *
     * @return the program runner class
     */
    public Class<? extends ProgramRunner> getRunnerClass() {
        return runnerClass;
    }

    /**
     * Sets the runner class.
     *
     * @param runnerClass the program runner class
     */
    protected void setRunnerClass(Class<? extends ProgramRunner> runnerClass) {
        this.runnerClass = runnerClass;
    }

    /**
     * Returns the old runner subclass name, if applicable. This information is only
     * necessary for looking up old preferences. The class does not need to exist.
     *
     * @return the old class name
     */
    public String getLegacyClass() {
        return legacyClass;
    }

    /**
     * Sets the old runner subclass name.
     *
     * @param legacyClass the old class name
     */
    protected void setLegacyClass(String legacyClass) {
        this.legacyClass = legacyClass;
    }

    /**
     * Returns if the runner is used by the core build process, or if it is a
     * utility runner, which contributes to the latex build.
     *
     * @return <code>true</code> for core runners (latex, pdflatex, ps2pdf etc.),
     *  <code>false</code> for utility runners (bibtex, makeindex etc.).
     */
    public boolean isCoreRunner() {
        return coreRunner;
    }

    /**
     * Sets if the current runner is used by the core build process.
     *
     * @param coreRunner
     */
    protected void setCoreRunner(boolean coreRunner) {
        this.coreRunner = coreRunner;
    }

    /**
     * Returns the default name of the program file. On Windows platforms, this also
     * includes the ".exe" extension.
     *
     * @return the program name
     */
    public String getExecutable() {
        return executable;
    }

    /**
     * Sets the default executable name.
     *
     * @param executable the program name
     */
    protected void setExecutable(String executable) {
        this.executable = executable;
    }

    /**
     * Returns the default program arguments used for initalizing the preferences, and
     * if not overwritten there, also for running the program.
     *
     * @return the default program arguments
     */
    public String getDefaultArguments() {
        return defaultArguments;
    }

    /**
     * Sets the default program arguments.
     *
     * @param defaultArguments the default program arguments
     */
    protected void setDefaultArguments(String defaultArguments) {
        this.defaultArguments = defaultArguments;
    }

}
