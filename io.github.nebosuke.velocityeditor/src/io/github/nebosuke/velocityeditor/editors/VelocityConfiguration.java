package io.github.nebosuke.velocityeditor.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import org.eclipse.core.resources.IFile;
import io.github.nebosuke.velocityeditor.assist.VelocityContentAssistProcessor;
import io.github.nebosuke.velocityeditor.assist.MacroDefinitionResolver;
import io.github.nebosuke.velocityeditor.preferences.ColorManager;
import io.github.nebosuke.velocityeditor.scanners.HTMLTagScanner;
import io.github.nebosuke.velocityeditor.scanners.ScriptBlockScanner;
import io.github.nebosuke.velocityeditor.scanners.VelocityCodeScanner;
import io.github.nebosuke.velocityeditor.scanners.VelocityCommentScanner;
import io.github.nebosuke.velocityeditor.scanners.VelocityPartitionScanner;

/**
 * Configuration for the Velocity editor
 * Sets up syntax highlighting, content assist, and double-click behavior
 */
public class VelocityConfiguration extends TextSourceViewerConfiguration {

    private final ColorManager colorManager;
    private final VelocityContentAssistProcessor contentAssistProcessor;
    private final MacroDefinitionResolver macroDefinitionResolver;
    private IProject project;
    private IFile file;
    private VelocityCodeScanner velocityCodeScanner;
    private HTMLTagScanner htmlTagScanner;
    private ScriptBlockScanner scriptBlockScanner;
    private VelocityCommentScanner commentScanner;
    private VelocityDoubleClickStrategy doubleClickStrategy;

    public VelocityConfiguration(ColorManager colorManager, IProject project) {
        this.colorManager = colorManager;
        this.project = project;
        this.contentAssistProcessor = new VelocityContentAssistProcessor(project);
        this.macroDefinitionResolver = new MacroDefinitionResolver();
    }

    public void setProject(IProject project) {
        this.project = project;
        this.contentAssistProcessor.setProject(project);
    }

    public void setFile(IFile file) {
        this.file = file;
    }

    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] {
            IDocument.DEFAULT_CONTENT_TYPE,
            VelocityPartitionScanner.VTL_COMMENT,
            VelocityPartitionScanner.VTL_MULTILINE_COMMENT,
            VelocityPartitionScanner.HTML_TAG,
            VelocityPartitionScanner.SCRIPT_BLOCK
        };
    }

    @Override
    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
        if (doubleClickStrategy == null) {
            doubleClickStrategy = new VelocityDoubleClickStrategy();
        }
        return doubleClickStrategy;
    }

    protected VelocityCodeScanner getVelocityCodeScanner() {
        if (velocityCodeScanner == null) {
            velocityCodeScanner = new VelocityCodeScanner(colorManager);
        }
        return velocityCodeScanner;
    }

    protected HTMLTagScanner getHTMLTagScanner() {
        if (htmlTagScanner == null) {
            htmlTagScanner = new HTMLTagScanner(colorManager);
        }
        return htmlTagScanner;
    }

    protected VelocityCommentScanner getCommentScanner() {
        if (commentScanner == null) {
            commentScanner = new VelocityCommentScanner(colorManager);
        }
        return commentScanner;
    }

    protected ScriptBlockScanner getScriptBlockScanner() {
        if (scriptBlockScanner == null) {
            scriptBlockScanner = new ScriptBlockScanner(colorManager);
        }
        return scriptBlockScanner;
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        PresentationReconciler reconciler = new PresentationReconciler();

        // Default content (VTL code + plain text)
        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getVelocityCodeScanner());
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        // VTL single-line comment
        DefaultDamagerRepairer commentDr = new DefaultDamagerRepairer(getCommentScanner());
        reconciler.setDamager(commentDr, VelocityPartitionScanner.VTL_COMMENT);
        reconciler.setRepairer(commentDr, VelocityPartitionScanner.VTL_COMMENT);

        // VTL multi-line comment
        reconciler.setDamager(commentDr, VelocityPartitionScanner.VTL_MULTILINE_COMMENT);
        reconciler.setRepairer(commentDr, VelocityPartitionScanner.VTL_MULTILINE_COMMENT);

        // HTML tags
        DefaultDamagerRepairer htmlDr = new DefaultDamagerRepairer(getHTMLTagScanner());
        reconciler.setDamager(htmlDr, VelocityPartitionScanner.HTML_TAG);
        reconciler.setRepairer(htmlDr, VelocityPartitionScanner.HTML_TAG);

        // Script blocks (<script> ... </script>)
        DefaultDamagerRepairer scriptDr = new DefaultDamagerRepairer(getScriptBlockScanner());
        reconciler.setDamager(scriptDr, VelocityPartitionScanner.SCRIPT_BLOCK);
        reconciler.setRepairer(scriptDr, VelocityPartitionScanner.SCRIPT_BLOCK);

        return reconciler;
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        ContentAssistant assistant = new ContentAssistant();

        // Set processor for all content types
        assistant.setContentAssistProcessor(contentAssistProcessor, IDocument.DEFAULT_CONTENT_TYPE);
        assistant.setContentAssistProcessor(contentAssistProcessor, VelocityPartitionScanner.HTML_TAG);
        assistant.setContentAssistProcessor(contentAssistProcessor, VelocityPartitionScanner.SCRIPT_BLOCK);
        
        // Enable auto activation
        assistant.enableAutoActivation(true);
        assistant.setAutoActivationDelay(200);
        
        // Set characters that trigger auto activation
        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
        
        return assistant;
    }

    @Override
    public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
        return new IHyperlinkDetector[] {
            VelocityMacroHyperlinkDetector.createCommandClickDetector(macroDefinitionResolver, project, file)
        };
    }

    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return new VelocityMacroTextHover(macroDefinitionResolver, project, file);
    }

    @Override
    public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
        return new String[] { "##", "" };
    }

    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
        IAutoEditStrategy[] defaultStrategies = super.getAutoEditStrategies(sourceViewer, contentType);
        IAutoEditStrategy[] strategies = new IAutoEditStrategy[defaultStrategies.length + 1];
        System.arraycopy(defaultStrategies, 0, strategies, 0, defaultStrategies.length);
        strategies[defaultStrategies.length] = new TabToSpacesAutoEditStrategy();
        return strategies;
    }
}
