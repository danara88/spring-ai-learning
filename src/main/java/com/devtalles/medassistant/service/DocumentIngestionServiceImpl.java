package com.devtalles.medassistant.service;

import com.devtalles.medassistant.dto.IngestionResult;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Profile("!aws")
@Slf4j
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

	private final VectorStore openAiVectorStore;
	private final VectorStore ollamaVectorStore;

	@Value("classpath:docs/informacion-ibuprofeno.pdf")
	private Resource ibuprofenoDoc;
	@Value("classpath:docs/emergencias-cardiovasculares.pdf")
	private Resource cardiovascularDoc;
	@Value("classpath:docs/guia-de-manejo-de-hipertension-arterial.pdf")
	private Resource hipertensionDoc;

	public DocumentIngestionServiceImpl(@Qualifier("openAiVectorStore") VectorStore openAiVectorStore,
		@Qualifier("ollamaVectorStore") VectorStore ollamaVectorStore) {
		this.openAiVectorStore = openAiVectorStore;
		this.ollamaVectorStore = ollamaVectorStore;
	}

	/// PostConstruct: Para que la ingestion se ejecute al arrancar la aplicación. En producción debemos hacerlo en un
	/// proceso separado como un endpoint para ingestar. Si tenemos un error en RunTime, cambiar el modelo de embeddings.
	@PostConstruct
	public void ingest() {

		List<Document> docsExisting = openAiVectorStore.similaritySearch(
			SearchRequest.builder()
				.query("documento médico")
				.topK(1)
				.similarityThreshold(0.0) // Estamos buscando 1 documento en especifico
				.build()
		);

		if (! docsExisting.isEmpty()) {
			log.info("Documentos ya presentes en Chroma - omitiendo ingestión");
			return;
		}

		log.info("Beginning ingest");
		List<Document> allDocuments = new ArrayList<>();
		allDocuments.addAll(readDocument(ibuprofenoDoc));
		allDocuments.addAll(readDocument(cardiovascularDoc));
		allDocuments.addAll(readDocument(hipertensionDoc));
		log.info("Ingesting {} documents into OpenAI vector store", allDocuments.size());

		/// El splitter divide el texto en fragmentos (chunks) con 800 tokens y máximo 1000 tokens.
		/// El splitter nos permite evitar el overlaping entre fragmentos, evitando que se pierda contexto.
		/// Si el fragmento de 800 tokens hace un overlap, me paso a los 1000 tokens.
		TokenTextSplitter splitter = TokenTextSplitter.builder()
			// Cantidad de tokens aceptables para un trozo de texto.
			.withChunkSize(800)
			.withMaxNumChunks(1000)
			.build();
		List<Document> chunks = splitter.apply(allDocuments);

		log.info("Ingesting {} chunks into OpenAI vector store", chunks.size());
		/// Cada vectorStore genera sus propios vectores con su modelo de embeddings, y los guardar en sus
		/// respectivas colecciones en Chroma.
		openAiVectorStore.add(chunks);
		log.info("Ingesting {} chunks into Ollama vector store", chunks.size());
		/// Cada vectorStore genera sus propios vectores con su modelo de embeddings, y los guardar en sus
		/// respectivas colecciones en Chroma.
		ollamaVectorStore.add(chunks);
	}

	private List<Document> readDocument(Resource resource) {
		log.info("Reading documents from {}", resource.getFilename());
		TikaDocumentReader reader = new TikaDocumentReader(resource);
		return reader.get();
	}

	@Override
	public IngestionResult ingestNewDocument(Resource resource) {
		// 1. Leo el documento
		List<Document> documents = readDocument(resource);

		// 2. Definoi el tamaño de cada fragmento
		TokenTextSplitter splitter = TokenTextSplitter.builder()
			.withChunkSize(800)
			.withMaxNumChunks(1000)
			.build();

		// 3. Divido ese documento en diferentes fragmentos
		List<Document> chunks = splitter.apply(documents);

		log.info("Ingesting new document {} chunks into OpenAI vector store", chunks.size());

		// 4. Cargamos los fragmentos a cada uno de los vectores
		openAiVectorStore.add(chunks);
		ollamaVectorStore.add(chunks);

		return new IngestionResult(resource.getFilename(), chunks.size());
	}

}
