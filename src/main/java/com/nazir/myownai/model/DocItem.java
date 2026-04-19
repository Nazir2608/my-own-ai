package com.nazir.myownai.model;

public class DocItem {
    private int id;
    private String title;
    private String text;
    private float[] embedding;

    public DocItem() {}

    public DocItem(int id, String title, String text, float[] embedding) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.embedding = embedding;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }

    public int getWordCount() {
        return text == null ? 0 : text.split("\\s+").length;
    }

    public String getPreview() {
        if (text == null) return "";
        return text.length() > 120 ? text.substring(0, 120) + "…" : text;
    }
}
