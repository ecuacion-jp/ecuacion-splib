/*
 * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.ecuacion.splib.core.bl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.item.EclibItemContainer;
import jp.ecuacion.lib.core.util.PropertyFileUtil.Arg;
import jp.ecuacion.lib.core.util.ReflectionUtil;
import jp.ecuacion.lib.core.util.StringUtil;

/**
 * Supplies utilities.
 */
public class SplibCoreBl extends ReflectionUtil {

  /**
   * Throws an exception when duplicated. 
   * It only has a function to throw exception, not one to check the occuring of duplication.
   */
  protected static void throwExceptionWhenDuplicated(boolean isDuplicated,
      boolean checkFromAllGroups, String[] itemPropertyPaths, EclibItemContainer container)
      throws BizLogicAppException {
    List<String> itemNameKeyList = Arrays.asList(itemPropertyPaths).stream()
        .map(path -> container.getItem(path).getItemNameKey()).toList();

    throwExceptionWhenDuplicated(isDuplicated, checkFromAllGroups, itemPropertyPaths,
        itemNameKeyList.toArray(new String[itemNameKeyList.size()]));
  }

  /**
   * Throws an exception when duplicated. 
   * It only has a function to throw exception, not one to check the occuring of duplication.
   */
  protected static void throwExceptionWhenDuplicated(boolean isDuplicated,
      boolean checkFromAllGroups, String[] itemPropertyPaths, String[] itemNameKeys)
      throws BizLogicAppException {
    if (isDuplicated) {

      // The error message can be distinguished by using the value of
      // jp.ecuacion.splib.web.process-result-message.shown-at-the-top,
      // but when itemNameKeys has multiple values its message needs to tell the combination of
      // iutems, which means showing the item names is needed anyway so it doesn't mean much.
      String msgId = "jp.ecuacion.splib.core.service.SplibEditJpaService.message."
          + (itemPropertyPaths.length == 1 ? "duplicated" : "combinationDuplicated")
          + (checkFromAllGroups ? "InAllGroups" : "");

      String str = StringUtil.getSeparatedValuesString(itemNameKeys,
          "${+messages:jp.ecuacion.lib.core.common.itemName.separator}",
          "${+messages:jp.ecuacion.lib.core.common.itemName.prependParenthesis}${+item_names:",
          "}${+messages:jp.ecuacion.lib.core.common.itemName.appendParenthesis}", false);
      throw new BizLogicAppException(itemPropertyPaths, msgId,
          new Arg[] {Arg.formattedString(str)});
    }
  }

  /**
   * Offers duplicate check function.
   */
  protected <T> void internalDuplicateCheck(boolean checkFromAllGroups, List<T> entityList,
      EclibItemContainer rec, String itemNameKeyClass, String idItemPropertyPath,
      String... checkTargetItemPropertyPaths) throws BizLogicAppException {
    List<T> listWithoutMyself = entityList.stream().filter(
        e -> !getValue(e, idItemPropertyPath).toString().equals(getValue(rec, idItemPropertyPath)))
        .toList();

    for (String path : checkTargetItemPropertyPaths) {
      listWithoutMyself = listWithoutMyself.stream()
          .filter(e -> (getValue(e, path) == null && getValue(rec, path) == null)
              || getValue(e, path) != null
                  && getValue(e, path).toString().equals(getValue(rec, path)))
          .toList();
    }

    List<String> itemNameKeys = Arrays.asList(checkTargetItemPropertyPaths).stream()
        .map(path -> rec.getItem(path).getItemNameKey(itemNameKeyClass)).toList();

    SplibCoreBl.throwExceptionWhenDuplicated(listWithoutMyself.size() > 0, checkFromAllGroups,
        checkTargetItemPropertyPaths, itemNameKeys.toArray(new String[itemNameKeys.size()]));
  }

  /**
   * Offers child existence check with list.
   */
  public <T> void internalChildExistenceCheck(List<T> list, String entityMessageIdPart)
      throws BizLogicAppException {
    internalChildExistenceCheck(list, null, entityMessageIdPart);
  }

  /**
   * Offers child existence check with list.
   */
  public <T> void internalChildExistenceCheck(List<T> list, String messageId,
      String entityMessageIdPart) throws BizLogicAppException {
    internalChildExistenceCheck(list, messageId, entityMessageIdPart, null, null, null);
  }

  /**
   * Offers child existence check with list.
   */
  public <T> void internalChildExistenceCheck(List<T> list, String entityMessageIdPart,
      ChildExistenceCheckConditionBean... conditions) throws BizLogicAppException {
    internalChildExistenceCheck(list, null, entityMessageIdPart, conditions, null, null);
  }

  /**
   * Offers child existence check with list.
   */
  protected <T> void internalChildExistenceCheck(List<T> list, String messageId,
      String entityMessageIdPart, ChildExistenceCheckConditionBean[] conditions,
      String referingRecordDataLabel, String recordSpecifyingFieldName)
      throws BizLogicAppException {

    if (conditions != null) {
      for (ChildExistenceCheckConditionBean condition : conditions) {
        list = list.stream()
            .filter(e -> getValue(e, condition.itemPropertyPath).equals(condition.conditionValue))
            .toList();
      }
    }

    if (list.size() > 0) {
      String msgCmnPrefix = "jp.ecuacion.splib.core.bl.SplibCoreBl.message";
      String msgPrefix = msgCmnPrefix + ".cannotBeDeletedBecauseOfReference";
      String msgId = messageId != null ? messageId
          : msgPrefix + (recordSpecifyingFieldName == null ? "" : "WithDetails");

      String referingRecordDataLabelMsgIdPart =
          referingRecordDataLabel == null ? msgCmnPrefix + "Part.targetData"
              : referingRecordDataLabel;

      Arg[] args = recordSpecifyingFieldName == null ? new Arg[] {Arg.message(entityMessageIdPart)}
          : new Arg[] {Arg.message(entityMessageIdPart),
              Arg.string(referingRecordDataLabelMsgIdPart),
              Arg.string(getValue(list.get(0), recordSpecifyingFieldName).toString())};
      throw new BizLogicAppException(msgId, args);

    }
  }

  /**
   * Offers child existence check with optional.
   */
  public <T> void internalChildExistenceCheck(Optional<T> optional, String entityMessageIdPart)
      throws BizLogicAppException {
    internalChildExistenceCheck(optional, null, entityMessageIdPart);
  }

  /**
   * Offers child existence check with optional.
   */
  public <T> void internalChildExistenceCheck(Optional<T> optional, String messageId,
      String entityMessageIdPart) throws BizLogicAppException {
    internalChildExistenceCheck(optional, messageId, entityMessageIdPart, null, null, null);
  }

  /**
   * Offers child existence check with optional.
   */
  public <T> void internalChildExistenceCheck(Optional<T> optional, String entityMessageIdPart,
      ChildExistenceCheckConditionBean... conditions) throws BizLogicAppException {
    internalChildExistenceCheck(optional, null, entityMessageIdPart, conditions, null, null);
  }

  /**
   * Offers child existence check with optional.
   */
  protected <T> void internalChildExistenceCheck(Optional<T> optional, String messageId,
      String entityMessageIdPart, ChildExistenceCheckConditionBean[] conditions,
      String referingRecordDataLabel, String recordSpecifyingFieldName)
      throws BizLogicAppException {
    List<T> list = new ArrayList<>();

    if (optional.isPresent()) {
      list.add(optional.get());
    }

    internalChildExistenceCheck(list, null, entityMessageIdPart, conditions,
        referingRecordDataLabel, recordSpecifyingFieldName);
  }

  /**
   * Contains attributes for child existence check condition.
   */
  public static record ChildExistenceCheckConditionBean(String itemPropertyPath,
      Object conditionValue, String messageIdDesignatingWhatCannotBeDone) {
  }
}
