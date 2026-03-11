package br.com.guardian.backend.domain.classification

import br.com.guardian.backend.domain.events.Event
import br.com.guardian.backend.domain.events.EventRepository
import org.springframework.stereotype.Service

@Service
class ClassificationService(
    private val classifierClient: ClassifierClient,
    private val classificationRepository: ClassificationRepository
) {

    fun classify(event: Event): ClassificationResult {

        val (label, risk) =
            classifierClient.classify(event.title, event.urlHost)

        val result = ClassificationResult(
            event = event,
            model = "mock-v1",
            label = label,
            riskScore = risk,
            rationale = "Classificação automática baseada em heurística inicial"
        )

        return classificationRepository.save(result)
    }
}