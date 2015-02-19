package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

public class Format extends AbstractObject {

    private static final long serialVersionUID = 1L;

    Folder folder;
    
    List<FormatVersion> formatVersions;
        
    String name;
    
    String type;
    
    String folderId;
    
    boolean shared;

    public Format() {
    	formatVersions = new ArrayList<FormatVersion>();
    }
    
    public Format(Folder folder) {
    	this();
    	this.folder = folder;
    }
    
	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public List<FormatVersion> getFormatVersions() {
		return formatVersions;
	}

	public void setFormatVersions(List<FormatVersion> formatVersions) {
		this.formatVersions = formatVersions;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFolderId() {
		return folderId;
	}

	public void setFolderId(String folderId) {
		this.folderId = folderId;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

}