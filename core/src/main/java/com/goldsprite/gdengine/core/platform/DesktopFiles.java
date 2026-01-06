package com.goldsprite.gdengine.core.platform;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;

public class DesktopFiles implements Files {
	private final Files delegate;

	public DesktopFiles(Files delegate) {
		this.delegate = delegate;
	}

	@Override
	public FileHandle getFileHandle(String path, FileType type) {
		return new DesktopFileHandle(path, type);
	}

	@Override
	public FileHandle classpath(String path) { return delegate.classpath(path); }

	@Override
	public FileHandle internal(String path) {
		// 拦截 Internal，返回我们的增强版 Handle
		return new DesktopFileHandle(path, FileType.Internal);
	}

	@Override
	public FileHandle external(String path) { return delegate.external(path); }
	@Override
	public FileHandle absolute(String path) { return delegate.absolute(path); }
	@Override
	public FileHandle local(String path) { return delegate.local(path); }
	@Override
	public String getExternalStoragePath() { return delegate.getExternalStoragePath(); }
	@Override
	public boolean isExternalStorageAvailable() { return delegate.isExternalStorageAvailable(); }
	@Override
	public String getLocalStoragePath() { return delegate.getLocalStoragePath(); }
	@Override
	public boolean isLocalStorageAvailable() { return delegate.isLocalStorageAvailable(); }
}
