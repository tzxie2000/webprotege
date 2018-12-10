package edu.stanford.bmir.protege.web.server.change.matcher;

import com.google.common.collect.Streams;
import edu.stanford.bmir.protege.web.server.owlapi.OWLObjectStringFormatter;
import org.semanticweb.owlapi.change.AddAxiomData;
import org.semanticweb.owlapi.change.OWLOntologyChangeData;
import org.semanticweb.owlapi.change.RemoveAxiomData;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.vocab.SKOSVocabulary;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toSet;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2018-12-08
 */
@SuppressWarnings("Convert2MethodRef")
public class MergeEntitiesMatcher implements ChangeMatcher {

    @Nonnull
    private final OWLObjectStringFormatter formatter;

    @Inject
    public MergeEntitiesMatcher(@Nonnull OWLObjectStringFormatter formatter) {
        this.formatter = checkNotNull(formatter);
    }

    @Override
    public Optional<String> getDescription(List<OWLOntologyChangeData> changeData) {
        // Don't match simple edits matched by other stuff
        if(changeData.size() < 3) {
            return Optional.empty();
        }

        // Must contain removals and additions
        var removedAxiomsIriSignature = changeData.stream()
                .filter(data -> data instanceof RemoveAxiomData)
                .map(data -> ((RemoveAxiomData) data).getAxiom())
                .flatMap(axiom -> this.getIriSignature(axiom))
                .collect(toSet());
        var addedAxiomsIriSignature = changeData.stream()
                .filter(data -> data instanceof AddAxiomData)
                .map(data -> ((AddAxiomData) data).getAxiom())
                .flatMap(axiom -> this.getIriSignature(axiom))
                .collect(toSet());


        // Things that were introduced into the signature by axiom additions
        // If there is one thing in the signature that was introduced then
        // this is the candidate target entity in the merge
        var introducedSignature = new HashSet<>(addedAxiomsIriSignature);
        introducedSignature.removeAll(removedAxiomsIriSignature);
        // We introduce skos:altLabel in the merge, so ignore this
        introducedSignature.remove(SKOSVocabulary.ALTLABEL.getIRI());
        // Don't match apparent merges into owl:Thing
        introducedSignature.remove(OWLRDFVocabulary.OWL_THING.getIRI());
        if(introducedSignature.size() != 1) {
            return Optional.empty();
        }

        // Find the things that disappeared from the signature
        var removedSignature = new HashSet<>(removedAxiomsIriSignature);
        removedAxiomsIriSignature.removeAll(addedAxiomsIriSignature);
        // Ignore rdfs:label because this is removed from the merge
        removedSignature.remove(OWLRDFVocabulary.RDFS_LABEL.getIRI());
        if(removedAxiomsIriSignature.isEmpty()) {
            return Optional.empty();
        }
        var mergedEntity = removedSignature.iterator().next();
        var mergedIntoEntity = introducedSignature.iterator().next();
        return formatter.format("Merged %s into %s", mergedEntity, mergedIntoEntity);
    }

    private Stream<IRI> getIriSignature(OWLAxiom axiom) {
        if(axiom instanceof OWLAnnotationAssertionAxiom) {
            var entitySignature = axiom.getSignature().stream().map(entity -> entity.getIRI());
            var annotationAssertion = (OWLAnnotationAssertionAxiom) axiom;
            Stream<IRI> iriSignature;
            if(annotationAssertion.getSubject() instanceof IRI) {
                iriSignature = Stream.of((IRI) annotationAssertion.getSubject());
            }
            else {
                iriSignature = Stream.empty();
            }
            return Streams.concat(entitySignature, iriSignature);
        }
        else {
            return axiom.getSignature().stream().map(entity -> entity.getIRI());
        }
    }
}
