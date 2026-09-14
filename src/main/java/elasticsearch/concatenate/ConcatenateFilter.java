package elasticsearch.concatenate;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;

public final class ConcatenateFilter extends TokenFilter {

    private final static String DEFAULT_TOKEN_SEPARATOR = " ";

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private String tokenSeparator;
    private int incrementGap;
    
    private StringBuilder builder = new StringBuilder();
    private AttributeSource.State previousState = null;
    private boolean recheckPrevious = false;

    public ConcatenateFilter(TokenStream input, String tokenSeparator, int incrementGap) {
        super(input);
        this.tokenSeparator = tokenSeparator != null ? tokenSeparator : DEFAULT_TOKEN_SEPARATOR;
        this.incrementGap = incrementGap;
    }

    @Override
    public boolean incrementToken() throws IOException {
        builder.setLength(0);

        if (recheckPrevious) {
            restoreState(previousState);
            builder.append(termAtt.buffer(), 0, termAtt.length());
            recheckPrevious = false;
        }

        while (input.incrementToken()) {
            // If the builder is empty, we must accept the token to start the concatenation,
            // regardless of how large its position increment is.
            if (posIncrAtt.getPositionIncrement() <= incrementGap || builder.length() == 0) {
                if (builder.length() > 0) {
                    builder.append(tokenSeparator);
                }
                builder.append(termAtt.buffer(), 0, termAtt.length());
            } else {
                recheckPrevious = true;
                previousState = captureState();
                break;
            }
        }

        // If we gathered any characters, we successfully formed a token.
        if (builder.length() > 0) {
            termAtt.setEmpty().append(builder);
            return true; 
        }

        // If we reach here, the stream is truly exhausted.
        return false; 
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        
        // Prevent the StringBuilder from permanently holding onto massive arrays.
        // If it grows beyond a reasonable threshold (e.g., 2048 chars), replace it to let the GC clean it up.
        if (builder.capacity() > 2048) { 
            builder = new StringBuilder();
        } else {
            builder.setLength(0);
        }
        
        // Clear lingering state so it does not bleed into the next document
        previousState = null;
        recheckPrevious = false;
    }

    @Override
    public void close() throws IOException {
        super.close();
        builder = new StringBuilder(); // Free memory when the stream is completely closed
        previousState = null;
    }
}
