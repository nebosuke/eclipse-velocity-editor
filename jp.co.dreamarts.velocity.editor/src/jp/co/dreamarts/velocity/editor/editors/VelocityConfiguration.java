package jp.co.dreamarts.velocity.editor.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import jp.co.dreamarts.velocity.editor.assist.VelocityContentAssistProcessor;
import jp.co.dreamarts.velocity.editor.preferences.ColorManager;
import jp.co.dreamarts.velocity.editor.scanners.HTMLTagScanner;
import jp.co.dreamarts.velocity.editor.scanners.VelocityCodeScanner;
import jp.co.dreamarts.velocity.editor.scanners.VelocityCommentScanner;
import jp.co.dreamarts.velocity.editor.scanners.VelocityPartitionScanner;

/**
 * Configuration for the Velocity editor
 * Sets up syntax highlighting, content assist, and double-click behavior
 */
public class VelocityConfiguration extends TextSourceViewerConfiguration {

    private final ColorManager colorManager;
    private VelocityCodeScanner velocityCodeScanner;
    private HTMLTagScanner htmlTagScanner;
    private VelocityCommentScanner commentScanner;
    private VelocityDoubleClickStrategy doubleClickStrategy;

    public VelocityConfiguration(ColorManager colorManager) {
        this.colorManager = colorManager;
    }

    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] {
            IDocument.DEFAULT_CONTENT_TYPE,
            VelocityPartitionScanner.VTL_COMMENT,
            VelocityPartitionScanner.VTL_MULTILINE_COMMENT,
            VelocityPartitionScanner.HTML_TAG
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

        return reconciler;
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        ContentAssistant assistant = new ContentAssistant();
        
        VelocityContentAssistProcessor processor = new VelocityContentAssistProcessor();
        
        // Set processor for all content types
        assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
        assistant.setContentAssistProcessor(processor, VelocityPartitionScanner.HTML_TAG);
        
        // Enable auto activation
        assistant.enableAutoActivation(true);
        assistant.setAutoActivationDelay(200);
        
        // Set characters that trigger auto activation
        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
        
        return assistant;
    }

    @Override
    public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
        return new String[] { "##", "" };
    }
}
