package com.stavfx.quiktweet.model;

/**
 * Created by Stav on 11/20/13.
 */
public class TwitterStatus {
	private long id;
	private String text;
	private String link;

	public TwitterStatus() {
	}

	public TwitterStatus(long id, String text, String link) {
		this.id = id;
		this.text = text;
		this.link = link;
	}

	public TwitterStatus(long id, String text) {
		this.id = id;
		this.text = text;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TwitterStatus))
			return false;
		return ((TwitterStatus) o).id == this.id;
	}
}
