package it.gov.innovazione.ndc.harvester.pathprocessors;

import it.gov.innovazione.ndc.harvester.exception.SinglePathProcessingException;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModel;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Resource;

@RequiredArgsConstructor
@Slf4j
public abstract class BaseSemanticAssetPathProcessor<P extends SemanticAssetPath, M extends SemanticAssetModel> implements SemanticAssetPathProcessor<P> {
    private final TripleStoreRepository tripleStoreRepository;
    protected final SemanticAssetMetadataRepository metadataRepository;

    @Override
    public void process(String repoUrl, P path) {
        try {
            log.info("Processing path {}", path);

            log.debug("Loading model");
            M model = loadModel(path.getTtlPath(), repoUrl);

            log.debug("Extracting main resource");
            Resource resource = model.getMainResource();
            log.info("Found resource {}", resource);

            processWithModel(repoUrl, path, model);
            log.info("Path {} processed", path);
        } catch (Exception e) {
            log.error("Error processing {}", path, e);
            throw new SinglePathProcessingException(String.format("Cannot process '%s'", path), e);
        }
    }

    protected void processWithModel(String repoUrl, P path, M model) {
        log.debug("Enriching model before persisting");
        enrichModelBeforePersisting(model, path);
        indexMetadataForSearch(model);
        persistModelToTripleStore(repoUrl, path, model);
    }

    protected void enrichModelBeforePersisting(M model, P path) {
        // default implementation doesn't do anything.
        // if you need to enrich the RDF triples (e.g. add REST API URL property), you can override this method.
        // maybe call super() in there, anyways.
    }

    private void indexMetadataForSearch(M model) {
        log.debug("Indexing {} for search", model.getMainResource());
        SemanticAssetMetadata metadata = model.extractMetadata();
        metadataRepository.save(metadata);
    }

    private void persistModelToTripleStore(String repoUrl, P path, M model) {
        log.debug("Storing RDF content for {} in Virtuoso", model.getMainResource());
        tripleStoreRepository.save(repoUrl, model.getRdfModel());
    }

    protected abstract M loadModel(String ttlFile, String repoUrl);
}
