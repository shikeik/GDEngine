package com.goldsprite.gdengine.core.platform;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import com.goldsprite.gdengine.core.utils.AssetUtils;

import java.io.File;

/**
 * Desktop 端专用文件句柄
 * 解决了 JAR 包内无法 list() 的问题
 */
public class DesktopFileHandle extends FileHandle {

	public DesktopFileHandle(String fileName, FileType type) {
		super(fileName, type);
	}

	public DesktopFileHandle(File file, FileType type) {
		super(file, type);
	}

	// --- 核心重写：拦截 list ---

	@Override
	public FileHandle[] list() {
		if (type() == FileType.Internal) {
			// 1. 尝试从索引获取
			String[] names = AssetUtils.listNames(path());

			if (names.length > 0) {
				FileHandle[] handles = new FileHandle[names.length];
				for (int i = 0; i < names.length; i++) {
					handles[i] = child(names[i]);
				}
				return handles;
			}
			// 2. 如果索引没结果 (可能是在IDE且没生成assets.txt)，回退原生逻辑
		}

		// 原生逻辑 (注意：list() 返回的是 FileHandle[]，我们需要把它包装成 DesktopFileHandle)
		FileHandle[] rawHandles = super.list();
		if (rawHandles == null) return new FileHandle[0];

		FileHandle[] wrappedHandles = new FileHandle[rawHandles.length];
		for(int i=0; i<rawHandles.length; i++) {
			wrappedHandles[i] = new DesktopFileHandle(rawHandles[i].file(), type());
		}
		return wrappedHandles;
	}

	@Override
	public FileHandle[] list(String suffix) {
		// 简单实现：获取全量再过滤
		FileHandle[] all = list();
		if (suffix == null) return all;

		int count = 0;
		for (FileHandle h : all) {
			if (h.name().endsWith(suffix)) count++;
		}

		FileHandle[] filtered = new FileHandle[count];
		int idx = 0;
		for (FileHandle h : all) {
			if (h.name().endsWith(suffix)) filtered[idx++] = h;
		}
		return filtered;
	}

	// --- 传递性重写：确保子节点也是 DesktopFileHandle ---

	@Override
	public FileHandle child(String name) {
		if (file.getPath().isEmpty()) return new DesktopFileHandle(new File(name), type());
		return new DesktopFileHandle(new File(file, name), type());
	}

	@Override
	public FileHandle parent() {
		File parent = file.getParentFile();
		if (parent == null) {
			if (type() == FileType.Absolute) parent = new File("/");
			else parent = new File("");
		}
		return new DesktopFileHandle(parent, type());
	}

	@Override
	public FileHandle sibling(String name) {
		if (file.getPath().length() == 0) throw new com.badlogic.gdx.utils.GdxRuntimeException("Cannot get the sibling of the root.");
		return new DesktopFileHandle(new File(file.getParent(), name), type());
	}

	@Override
	public boolean isDirectory() {
		if (type() == FileType.Internal) {
			// 既然 AssetUtils 已经告诉我们这个 handle 是存在的
			// 我们尝试 list 一下，如果有子文件，那就是目录
			// 这是一个低成本的判断
			String[] children = AssetUtils.listNames(path());
			return children.length > 0;
		}
		return super.isDirectory();
	}
}
