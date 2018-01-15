package elasticsearch.concatenate;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.ESTokenStreamTestCase;

import java.io.IOException;
import java.io.StringReader;

import static org.elasticsearch.test.ESTestCase.createTestAnalysis;

public class ConcatenateFilterTest extends ESTokenStreamTestCase {

    public void testConcatenateFilter() throws IOException
    {
        Settings settings = Settings.builder()
                .put("index.analysis.filter.my_filter.type", "concatenate")
                .build();

        ESTestCase.TestAnalysis analysis = createTestAnalysis(new Index("test", "_na_"), settings, new ConcatenatePlugin());
        TokenFilterFactory filter = analysis.tokenFilter.get("my_filter");
        Tokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader("next to"));
        TokenStream tokenStream = filter.create(tokenizer);
        BaseTokenStreamTestCase.assertTokenStreamContents(tokenStream, new String[] { "next to" });

    }

    public void testConcatenateFilterIncrementGap() throws IOException
    {
        Settings settings = Settings.builder()
                .put("index.analysis.filter.my_filter.type", "concatenate")
                .put("index.analysis.filter.my_filter.increment_gap", "100")
                .build();

        ESTestCase.TestAnalysis analysis = createTestAnalysis(new Index("test", "_na_"), settings, new ConcatenatePlugin());
        TokenFilterFactory filter = analysis.tokenFilter.get("my_filter");
        Tokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader("dans les environs"));
        TokenStream tokenStream = filter.create(tokenizer);
        BaseTokenStreamTestCase.assertTokenStreamContents(tokenStream, new String[] { "dans les environs" });

    }
}