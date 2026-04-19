
### Production-Grade Vector Database with RAG Pipeline

*Built from scratch in Java 21 + Spring Boot*

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[Features](#-features) • [Quick Start](#-quick-start) • [Architecture](#-architecture) • [API](#-api-reference) • [Themes](#-themes) • [Docker](#-docker-deployment)

<img src="docs/screenshot.png" alt="MY-OWN-AI Screenshot" width="800"/>

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [How It Works](#-how-it-works)
- [Quick Start](#-quick-start)
- [Architecture](#-architecture)
- [Search Algorithms](#-search-algorithms)
- [RAG Pipeline](#-rag-pipeline)
- [API Reference](#-api-reference)
- [Themes](#-themes)
- [Docker Deployment](#-docker-deployment)
- [Configuration](#-configuration)
- [Troubleshooting](#-troubleshooting)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Overview

**MY-OWN-AI** is a fully functional vector database built from scratch to demonstrate how production systems like Pinecone, Weaviate, and Chroma work under the hood. It implements three search algorithms side-by-side, features a complete RAG (Retrieval Augmented Generation) pipeline, and includes a beautiful interactive web UI with 6 customizable themes.

### Why This Project?

- **Educational**: Understand how vector databases actually work
- **Production-Ready**: HNSW implementation matches industry standards
- **Complete Stack**: Backend (Java/Spring), Frontend (Vanilla JS), AI (Ollama)
- **Fully Functional**: Not a toy—handles real embeddings and RAG workflows
- **Beautiful UI**: 6 themes, responsive design, smooth animations

---

## ✨ Features

### 🔍 Search Algorithms

| Algorithm | Time Complexity | Accuracy | Use Case |
|-----------|----------------|----------|----------|
| **HNSW** (Hierarchical Navigable Small World) | O(log N) | 95-99% | Production (high-dimensional data) |
| **KD-Tree** (K-Dimensional Tree) | O(log N) | 100% | Low-dimensional data (≤20D) |
| **Brute Force** | O(N) | 100% | Baseline/ground truth |

### 📊 Distance Metrics

- **Cosine Similarity** - Text embeddings (direction matters)
- **Euclidean Distance** - General purpose (magnitude matters)
- **Manhattan Distance** - Sparse data, outlier-resistant

### 🎨 UI Features

- **6 Beautiful Themes**
    - Dark Blue (default)
    - Dark Purple
    - Cyberpunk
    - Nord
    - Dracula
    - Light Mode

- **Interactive Visualization**
    - 2D PCA scatter plot
    - Real-time search highlighting
    - Semantic clustering visualization
    - Hover tooltips

- **Enhanced RAG Output**
    - Clean typography (Inter + Fira Code fonts)
    - Proper paragraph formatting
    - Smooth typewriter effect
    - Context chip interactions

### 🤖 AI Features

- **Real Embeddings** via Ollama's `nomic-embed-text` (768D)
- **Local LLM Generation** via `gemma3:4b`
- **Automatic Text Chunking** (250 words, 30-word overlap)
- **Semantic Search** with HNSW indexing
- **Context Retrieval** for accurate answers

### 🐳 DevOps

- **Docker Compose** setup
- **Multi-stage builds** for optimized images
- **Health checks** and auto-restart
- **Persistent volumes** for Ollama models
- **Production-ready** configurations
