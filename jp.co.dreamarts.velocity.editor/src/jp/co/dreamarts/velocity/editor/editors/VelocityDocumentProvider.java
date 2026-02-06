package jp.co.dreamarts.velocity.editor.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

import jp.co.dreamarts.velocity.editor.scanners.VelocityPartitionScanner;

/**
 * Document provider for Velocity files
 * Sets up document partitioning for syntax highlighting
 */
public class VelocityDocumentProvider extends FileDocumentProvider {

    @Override
    protected IDocument createDocument(Object element) throws CoreException {
        IDocument document = super.createDocument(element);
        if (document != null) {
            IDocumentPartitioner partitioner = new FastPartitioner(
                    new VelocityPartitionScanner(),
                    VelocityPartitionScanner.PARTITION_TYPES);
            partitioner.connect(document);
            document.setDocumentPartitioner(partitioner);
        }
        return document;
    }
}
