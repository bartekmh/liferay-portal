/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.kernel.util;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.io.ByteArrayFileInputStream;
import com.liferay.portal.kernel.io.FileFilter;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portlet.documentlibrary.store.DLStoreUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Sergio González
 */
public class TempFileUtil {

	public static String addTempFile(
			long userId, String tempPathName, File file)
		throws IOException, PortalException, SystemException {

		String tempFileName = FileUtil.createTempFileName();

		DLStoreUtil.validate(tempFileName, true, file);

		File tempFile = getTempFile(tempFileName, tempPathName);

		FileUtil.copyFile(file, tempFile);

		return tempFileName;
	}

	public static String addTempFile(
			long userId, String fileName, String tempPathName, File file)
		throws IOException, PortalException, SystemException {

		DLStoreUtil.validate(fileName, true, file);

		File tempFile = getTempFile(userId, fileName, tempPathName);

		FileUtil.copyFile(file, tempFile);

		return fileName;
	}

	public static String addTempFile(
			long userId, String fileName, String tempPathName,
			InputStream inputStream)
		throws IOException, PortalException, SystemException {

		File file = null;

		if (inputStream instanceof ByteArrayFileInputStream) {
			ByteArrayFileInputStream byteArrayFileInputStream =
				(ByteArrayFileInputStream)inputStream;

			file = byteArrayFileInputStream.getFile();

			DLStoreUtil.validate(fileName, true, file);
		}
		else {
			DLStoreUtil.validate(fileName, true, inputStream);
		}

		File tempFile = getTempFile(userId, fileName, tempPathName);

		if (file != null) {
			FileUtil.copyFile(file, tempFile);
		}
		else {
			FileUtil.write(tempFile, inputStream);
		}

		return fileName;
	}

	public static String addTempFile(String tempPathName, File file)
		throws IOException, PortalException, SystemException {

		String tempFileName = FileUtil.createTempFileName();

		DLStoreUtil.validate(tempFileName, false, file);

		File tempFile = getTempFile(tempFileName, tempPathName);

		FileUtil.copyFile(file, tempFile);

		return tempFileName;
	}

	public static String addTempFile(
			String fileName, String tempPathName, File file)
		throws IOException, PortalException, SystemException {

		DLStoreUtil.validate(fileName, true, file);

		File tempFile = getTempFile(fileName, tempPathName);

		FileUtil.copyFile(file, tempFile);

		return fileName;
	}

	public static void deleteTempFile(
			long userId, String fileName, String tempPathName)
		throws PortalException {

		File file = getTempFile(userId, fileName, tempPathName);

		FileUtil.delete(file);
	}

	public static void deleteTempFile(String fileName, String tempPathName)
		throws PortalException {

		File file = getTempFile(fileName, tempPathName);

		FileUtil.delete(file);
	}

	public static File getTempFile(
			long userId, String fileName, String tempPathName)
		throws PortalException {

		try {
			String absoluteFilePath = getTempAbsolutePath(
				userId, fileName, tempPathName);

			return new File(absoluteFilePath);
		}
		catch (TempFileNameException e) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to return file " + fileName + " for " + userId +
						" in " + tempPathName,
					e);
			}

			throw new RuntimeException(e);
		}
	}

	public static File getTempFile(String fileName, String tempPathName)
		throws PortalException {

		try {
			String absoluteFilePath = getTempAbsolutePath(
				fileName, tempPathName);

			return new File(absoluteFilePath);
		}
		catch (TempFileNameException e) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to return file " + fileName + " in " +
						tempPathName,
					e);
			}

			throw new RuntimeException(e);
		}
	}

	public static String[] getTempFileEntryNames(
		long userId, String tempPathName) {

		try {
			File dir = new File(getTempAbsolutePath(tempPathName));

			StringBundler sb = new StringBundler(5);

			sb.append(StringPool.PERIOD);
			sb.append(StringPool.STAR);
			sb.append(StringPool.UNDERLINE);
			sb.append(userId);
			sb.append(_SUFFIX_TEMP_FILENAME_USERID_REGEX);

			FileFilter fileFilter = new FileFilter(sb.toString());

			File[] files = dir.listFiles(fileFilter);

			int count = 0;

			if (files != null) {
				count = files.length;
			}

			String[] fileNames = new String[count];

			for (int i = 0; i < count; i++) {
				File file = files[i];

				String fileName = StringUtil.replace(
					file.getName(),
					StringPool.UNDERLINE + userId + _SUFFIX_TEMP_FILENAME,
					StringPool.BLANK);

				fileNames[i] = fileName;
			}

			return fileNames;
		}
		catch (TempFileNameException e) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to list temporary file names for " + userId +
						" in " + tempPathName,
					e);
			}

			return new String[0];
		}
	}

	public static String[] getTempFileEntryNames(String tempPathName)
		throws TempFileNameException {

		File dir = new File(getTempAbsolutePath(tempPathName));

		File[] files = dir.listFiles(
			new FileFilter(_SUFFIX_TEMP_FILENAME_REGEX));

		String[] fileNames = new String[files.length];

		for (int i = 0; i < files.length; i++) {
			File file = files[i];

			String fileName = StringUtil.replace(
				file.getName(), _SUFFIX_TEMP_FILENAME, StringPool.BLANK);

			fileNames[i] = fileName;
		}

		return fileNames;
	}

	protected static boolean isValidFileName(String name) {
		if ((name == null) ||
			name.contains(StringPool.SLASH) ||
			name.contains(StringPool.BACK_SLASH)) {

			return false;
		}

		return true;
	}

	protected static void validatePathName(String pathName)
		throws TempFileNameException {

		if (pathName == null) {
			return;
		}

		if (pathName.indexOf(_NULL_CHAR) > -1) {
			throw new TempFileNameException();
		}

		int pos = pathName.indexOf(StringPool.DOUBLE_PERIOD);

		if (pos > -1) {
			if (pathName.length() == 2) {
				throw new TempFileNameException();
			}

			if (pos > 0) {
				char c = pathName.charAt(pos - 1);

				if ((c == CharPool.BACK_SLASH) || (c == CharPool.SLASH)) {
					throw new TempFileNameException();
				}
			}

			if ((pos + 2) < pathName.length()) {
				char c = pathName.charAt(pos + 2);

				if ((c == CharPool.BACK_SLASH) || (c == CharPool.SLASH)) {
					throw new TempFileNameException();
				}
			}
		}
	}

	private static String getTempAbsolutePath(
			long userId, String fileName, String tempPathName)
		throws PortalException {

		validatePathName(tempPathName);

		StringBundler sb = new StringBundler(5);

		sb.append(SystemProperties.get(SystemProperties.TMP_DIR));
		sb.append(_BASE_TEMP_PATHNAME);
		sb.append(tempPathName);
		sb.append(StringPool.SLASH);
		sb.append(getTempFileName(userId, fileName));

		return sb.toString();
	}

	private static String getTempAbsolutePath(String tempPathName)
		throws TempFileNameException {

		validatePathName(tempPathName);

		StringBundler sb = new StringBundler(4);

		sb.append(SystemProperties.get(SystemProperties.TMP_DIR));
		sb.append(_BASE_TEMP_PATHNAME);
		sb.append(tempPathName);
		sb.append(StringPool.SLASH);

		return sb.toString();
	}

	private static String getTempAbsolutePath(
			String fileName, String tempPathName)
		throws PortalException {

		validatePathName(tempPathName);

		StringBundler sb = new StringBundler(5);

		sb.append(SystemProperties.get(SystemProperties.TMP_DIR));
		sb.append(_BASE_TEMP_PATHNAME);
		sb.append(tempPathName);
		sb.append(StringPool.SLASH);
		sb.append(getTempFileName(fileName));

		return sb.toString();
	}

	private static String getTempFileName(long userId, String fileName)
		throws PortalException {

		if (!isValidFileName(fileName)) {
			throw new TempFileNameException();
		}

		StringBundler sb = new StringBundler(4);

		sb.append(fileName);
		sb.append(StringPool.UNDERLINE);
		sb.append(userId);
		sb.append(_SUFFIX_TEMP_FILENAME);

		return sb.toString();
	}

	private static String getTempFileName(String fileName)
		throws PortalException {

		if (!isValidFileName(fileName)) {
			throw new TempFileNameException();
		}

		return fileName + _SUFFIX_TEMP_FILENAME;
	}

	private static final String _BASE_TEMP_PATHNAME = "/liferay/";

	private static final char _NULL_CHAR = 0;

	private static final String _SUFFIX_TEMP_FILENAME = "_temp.tmp";

	private static final String _SUFFIX_TEMP_FILENAME_REGEX = ".*_temp\\.tmp";

	private static final String _SUFFIX_TEMP_FILENAME_USERID_REGEX =
		"_temp\\.tmp";

	private static Log _log = LogFactoryUtil.getLog(TempFileUtil.class);

}