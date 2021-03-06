/*
 * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arialyy.aria.core.download;

import android.text.TextUtils;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.inf.ICheckEntityUtil;
import com.arialyy.aria.core.inf.IOptionConstant;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import java.io.File;

public class CheckFtpDirEntityUtil implements ICheckEntityUtil {
  private final String TAG = "CheckFtpDirEntityUtil";
  private DGTaskWrapper mWrapper;
  private DownloadGroupEntity mEntity;
  private int action;

  public static CheckFtpDirEntityUtil newInstance(DGTaskWrapper wrapper, int action) {
    return new CheckFtpDirEntityUtil(wrapper, action);
  }

  private CheckFtpDirEntityUtil(DGTaskWrapper wrapper, int action) {
    this.action = action;
    mWrapper = wrapper;
    mEntity = mWrapper.getEntity();
  }

  /**
   * 检查并设置文件夹路径
   *
   * @return {@code true} 合法
   */
  private boolean checkDirPath() {
    if (TextUtils.isEmpty(mWrapper.getDirPathTemp())) {
      ALog.e(TAG, "文件夹路径不能为null");
      return false;
    } else if (!mWrapper.getDirPathTemp().startsWith("/")) {
      ALog.e(TAG, String.format("文件夹路径【%s】错误", mWrapper.getDirPathTemp()));
      return false;
    }
    File file = new File(mWrapper.getDirPathTemp());
    if (file.isFile()) {
      ALog.e(TAG, String.format("路径【%s】是文件，请设置文件夹路径", mWrapper.getDirPathTemp()));
      return false;
    }

    if ((mEntity.getDirPath() == null || !mEntity.getDirPath().equals(mWrapper.getDirPathTemp()))
        && DbEntity.checkDataExist(DownloadGroupEntity.class, "dirPath=?",
        mWrapper.getDirPathTemp())) {
      ALog.e(TAG, String.format("文件夹路径【%s】已被其它任务占用，请重新设置文件夹路径", mWrapper.getDirPathTemp()));
      return false;
    }

    if (TextUtils.isEmpty(mEntity.getDirPath()) || !mEntity.getDirPath()
        .equals(mWrapper.getDirPathTemp())) {
      if (!file.exists()) {
        file.mkdirs();
      }
      mEntity.setDirPath(mWrapper.getDirPathTemp());
      ALog.i(TAG, String.format("文件夹路径改变，将更新文件夹路径为：%s", mWrapper.getDirPathTemp()));
    }
    return true;
  }

  @Override
  public boolean checkEntity() {
    if (mWrapper.getErrorEvent() != null) {
      ALog.e(TAG, String.format("下载失败，%s", mWrapper.getErrorEvent().errorMsg));
      return false;
    }

    boolean b = checkDirPath() && checkUrl();
    if (b) {
      mEntity.save();
    }
    FtpUrlEntity urlEntity =
        (FtpUrlEntity) mWrapper.getOptionParams().getParam(IOptionConstant.ftpUrlEntity);
    assert urlEntity != null;
    if (urlEntity.isFtps) {
      if (TextUtils.isEmpty(urlEntity.storePath)) {
        ALog.e(TAG, "证书路径为空");
        return false;
      }
      if (TextUtils.isEmpty(urlEntity.keyAlias)) {
        ALog.e(TAG, "证书别名为空");
        return false;
      }
    }
    return b;
  }

  /**
   * 检查普通任务的下载地址
   *
   * @return {@code true}地址合法
   */
  private boolean checkUrl() {
    final String url = mEntity.getKey();
    if (TextUtils.isEmpty(url)) {
      ALog.e(TAG, "下载失败，url为null");
      return false;
    } else if (!url.startsWith("ftp")) {
      ALog.e(TAG, "下载失败，url【" + url + "】错误");
      return false;
    }
    int index = url.indexOf("://");
    if (index == -1) {
      ALog.e(TAG, "下载失败，url【" + url + "】不合法");
      return false;
    }
    return true;
  }
}
