package net.sourceforge.texlipse.builder;

import java.util.LinkedList;
import java.util.Set;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.model.PackageContainer;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Detects and automatically generates the cycle for a complete latex build
 * process. It uses a project file tracking instance for monitoring which files
 * are being modified in the source folder. Additionally, it detects which
 * files are in return read by latex, and which additional runners (e.g.
 * bibtex) need to be triggered.
 *
 * @author Matthias Erll
 *
 */
public class BuildCycleDetector {

    private final IProject project;
    private final IResource resource;
    private final ProjectFileTracking fileTracking;
    private final LinkedList<ProgramRunner> runners;
    private final IContainer sourceContainer;
    private final String[] tempExts;
    private final String[] addExts;
    private final int totalMax;

    private boolean biblatexMode;
    private String biblatexBackend;

    private int totalCount;
    private boolean done;

    /**
     * Checks if a runner with the given id is scheduled already.
     *
     * @param id runner id
     * @return <code>true</code> if the runner is scheduled, <code>false</code> otherwise
     */
    private boolean isRunnerQueued(final String id) {
        for (ProgramRunner runner : runners) {
            if (runner.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Schedules a program runner to run, i.e. moves it to the list of runners.
     * Avoids duplicates.
     *
     * @param runner program runner
     * @param push whether to move the runner to the beginning of the list
     */
    private void queueRunner(ProgramRunner runner, boolean push) {
        // If already scheduled, there is no use for scheduling
        // it again
        if (!isRunnerQueued(runner.getId())) {
            if (push) {
                // If scheduled later, move to front of the list
                runners.addFirst(runner);
            }
            else {
                runners.add(runner);
            }
        }
    }

    /**
     * Provides additional variables for deciding whether biblatex is loaded and
     * if the biber or biblatex backend is used.
     *
     * @param resource resource which is being built
     * @param packageContainer container with detected packages
     */
    private void detectBibConfig(final IResource resource,
            final PackageContainer packageContainer) {
        Boolean blMode = (Boolean) TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_BIBLATEXMODE_PROPERTY);
        biblatexMode = Boolean.TRUE.equals(blMode);
        biblatexBackend = (String) TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_BIBLATEXBACKEND_PROPERTY);
    
        if (packageContainer.hasPackage("biblatex")) {
            biblatexMode = true;
    
            if (biblatexBackend == null) {
                final String baseFileName = OutputFileManager.stripFileExt(
                        resource.getProjectRelativePath().toPortableString(), ".tex");
                final String blxFileName = baseFileName.concat("-blx.bib");
                final String bcfFileName = baseFileName.concat(".bcf");
                final IFile blxFile = project.getFile(blxFileName);
                final IFile bcfFile = project.getFile(bcfFileName);
                long blxTimestamp = blxFile.exists() ? blxFile.getLocalTimeStamp() : -1;
                long bcfTimestamp = bcfFile.exists() ? bcfFile.getLocalTimeStamp() : -1;
    
                if (blxTimestamp < bcfTimestamp) {
                    biblatexBackend = "biber";
                }
            }
        }
    }

    /**
     * Checks whether session variables indicate that bibtex or biblatex needs to run. This should
     * only be done once per build cycle, since otherwise it leads to infinite loops if citations
     * are undefined. 
     */
    private void checkBibtexVariables() {
        final String bibRerun = (String) TexlipseProperties.getSessionProperty(
                project, TexlipseProperties.SESSION_BIBTEX_RERUN);
        final Boolean bibFilesChanged = (Boolean) TexlipseProperties.getSessionProperty(
                project, TexlipseProperties.BIBFILES_CHANGED);
        if ("true".equals(bibRerun)
                || (Boolean.TRUE.equals(bibFilesChanged))) {
            final ProgramRunner bibRunner;
            if (biblatexMode && biblatexBackend != null) {
                bibRunner = BuilderRegistry.getRunner(biblatexBackend);
            }
            else {
                bibRunner = BuilderRegistry.getRunner(
                        TexlipseProperties.INPUT_FORMAT_BIB,
                        TexlipseProperties.OUTPUT_FORMAT_AUX);
            }
            TexlipseProperties.setSessionProperty(
                    project, TexlipseProperties.SESSION_BIBTEX_RERUN, null);
            TexlipseProperties.setSessionProperty(
                    project, TexlipseProperties.BIBFILES_CHANGED, null);
            if (bibRunner != null) {
                queueRunner(bibRunner, true);
                done = false;
            }
        }
    }

    /**
     * Checks if the latex runner has set any session variables, which affect the
     * build process.
     */
    private void checkLatexOutputVariables() {
        final String latexRerun = (String) TexlipseProperties.getSessionProperty(
                project, TexlipseProperties.SESSION_LATEX_RERUN);
        // Initialize with values from log output
        if ("true".equals(latexRerun)) {
            done = false;
        }
        TexlipseProperties.setSessionProperty(
                project, TexlipseProperties.SESSION_LATEX_RERUN, null);
    }

    /**
     * Checks the given latex or runner changed output files for their consequences
     * regarding the required build process:
     * <ul>
     * <li>If a runner for this file exists, it is scheduled to be started.</li>
     * <li>Independent from other runners, if latex re-reads this file (e.g.
     *  toc files), another latex run is scheduled.
     * </ul>
     *
     * @param changedOutputFiles set of files which have been modified during a
     *  recent run
     * @param push if set to <code>true</code>, runners which need to be triggered
     *  are scheduled before any other waiting runners. Otherwise they are moved
     *  to be back of the queue, but still started before the next latex process
     */
    private void checkOutputFiles(final Set<IPath> changedOutputFiles, boolean push) {
        final Set<IPath> inputFiles = AbstractLatexBuilder.getInputFiles(project);
        for (IPath changedFile : changedOutputFiles) {
            final String fileExt = changedFile.getFileExtension().toLowerCase();
            if (fileExt != null && fileExt.length() > 0
                    && !"tex".equals(fileExt) && !"bib".equals(fileExt)) {
                ProgramRunner runner = BuilderRegistry.getRunner(fileExt, null);
                if (runner != null) {
                    // Schedule runner before next LaTeX rebuild, if any
                    queueRunner(runner, push);
                }
                if (inputFiles.contains(changedFile)) {
                    // LaTeX incremental build, needs another run-through
                    done = false;
                }
            }
        }
    }

    /**
     * Constructor.
     *
     * @param project current project
     * @param resource resource to be built
     * @param fileTracking project file tracking instance
     */
    public BuildCycleDetector(final IProject project, final IResource resource,
            final ProjectFileTracking fileTracking) {
        this.project = project;
        this.resource = resource;
        this.fileTracking = fileTracking;
        this.runners = new LinkedList<ProgramRunner>();
        this.sourceContainer = resource.getParent();
        this.totalMax = TexlipsePlugin.getDefault().getPreferenceStore()
                .getInt(TexlipseProperties.BUILD_CYCLE_MAX);
        this.tempExts = TexlipsePlugin.getPreferenceArray(
                TexlipseProperties.TEMP_FILE_EXTS);
        this.addExts = TexlipsePlugin.getPreferenceArray(
                TexlipseProperties.BUILD_CYCLE_ADD_EXTS);
        this.totalCount = 0;
        this.done = true;
    }

    /**
     * Reads and initializes the hash values for detecting file modifications.
     * Additionally, session variables for bibtex / biblatex are evaluated.
     * If file changes have been made since the cache was stored (or the cache has been
     * initial), this can also lead to scheduling runners. However, runners are
     * not run before the first LaTeX run-through.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void initFileTracking(IProgressMonitor monitor) throws CoreException {
        Set<IPath> initialChanges = fileTracking.initFileCache(tempExts, addExts, monitor);
        checkOutputFiles(initialChanges, false);
    }

    /**
     * Make the file tracking persistent, in case the session gets closed.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void saveFileTracking(IProgressMonitor monitor) throws CoreException {
        fileTracking.saveFileCache(monitor);
    }

    /**
     * Returns the next runner, which is scheduled to run. This should be
     * called until <code>null</code> is returned.
     *
     * @return runner which is next in line to start
     */
    public ProgramRunner getNextRunner() {
        ProgramRunner nextRunner = runners.poll();
        return nextRunner;
    }

    /**
     * Checks the latex output (i.e. the FLS file) for input and output files,
     * compares this to actual changes in the source folder. If necessary, latex
     * and other runners are scheduled to be run again. This list of necessary
     * activities might get modified later when analyzed further runner output.
     * Latex is only re-run to a certain maximum count, to avoid infinite loops.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void checkLatexOutput(IProgressMonitor monitor) throws CoreException {
        // Check if the limit of build cycles has been reached
        totalCount++;
        if (isMaxedOut()) {
            done = true;
            return;
        }
        else {
            // Initialize depending on queued runners
            done = runners.isEmpty();
        }

        // Check latex output for additional info
        checkLatexOutputVariables();

        final Set<IPath> changedContentFiles = fileTracking.updateChangedFiles(
                sourceContainer, tempExts, addExts, monitor);

        checkOutputFiles(changedContentFiles, false);
    }

    /**
     * Does the initial checking, using <code>checkLatexOutput(IProgressMonitor)</code>,
     * but is used only after the intial latex run for checking additional variables.
     *
     * @param monitor progress monitor
     * @throws CoreException
     */
    public void checkInitialLatexOutput(IProgressMonitor monitor) throws CoreException {
        checkLatexOutput(monitor);
        final Object packageContainer = TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.PACKAGECONTAINER_PROPERTY);
        if (packageContainer != null) {
            detectBibConfig(resource, (PackageContainer) packageContainer);
        }
        checkBibtexVariables();
    }

    /**
     * Checks the project source folder for changes in the file system, schedules
     * more runners, and also more latex run-throughs if found necessary.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void checkRunnerOutput(IProgressMonitor monitor) throws CoreException {
        final Set<IPath> changedContentFiles = fileTracking.updateChangedFiles(
                sourceContainer, tempExts, addExts, monitor);
        checkOutputFiles(changedContentFiles, true);
    }

    /**
     * Returns <code>false</code> if latex should be run at least once more. If
     * it returns <code>true</code> this can either mean the latex build process
     * is completed, or that the set maximum of latex build cycles has been
     * reached.
     *
     * @return if latex should run again
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Returns if the maximum amount of build cycles has been reached.
     *
     * @return if the counted amount of latex run-throughs is equal or larger
     *  than the desired maximum
     */
    public boolean isMaxedOut() {
        return totalCount >= totalMax;
    }

}
