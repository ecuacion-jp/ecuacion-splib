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
  protected <T> void internalChildExistenceCheck(List<T> list, String referingEntityMessageId)
      throws BizLogicAppException {
    internalChildExistenceCheck(list, referingEntityMessageId, null, null, null, null);
  }

  /**
   * Offers child existence check with list.
   */
  protected <T> void internalChildExistenceCheck(List<T> list, String referingEntityMessageId,
      ChildExistenceCheckConditionBean[] conditions, String whatCannotBeDoneMessageId,
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
      String whatCannotBeDoneMsgIdPart =
          whatCannotBeDoneMessageId == null ? msgCmnPrefix + "Part.deleted"
              : whatCannotBeDoneMessageId;
      String referingRecordDataLabelMsgIdPart =
          referingRecordDataLabel == null ? msgCmnPrefix + "Part.targetData"
              : referingRecordDataLabel;

      Arg[] args = recordSpecifyingFieldName == null
          ? new Arg[] {Arg.message(referingEntityMessageId)}
          : new Arg[] {Arg.message(referingEntityMessageId), Arg.message(whatCannotBeDoneMsgIdPart),
              Arg.string(referingRecordDataLabelMsgIdPart),
              Arg.string(getValue(list.get(0), recordSpecifyingFieldName).toString())};
      throw new BizLogicAppException(
          msgPrefix + (recordSpecifyingFieldName == null ? "" : "WithDetails"), args);

    }
  }

  /**
   * Offers child existence check with optional.
   */
  protected <T> void internalChildExistenceCheck(Optional<T> optional,
      String referingEntityMessageId) throws BizLogicAppException {
    internalChildExistenceCheck(optional, referingEntityMessageId, null, null, null, null);
  }

  /**
   * Offers child existence check with optional.
   */
  protected <T> void internalChildExistenceCheck(Optional<T> optional,
      String referingEntityMessageId, ChildExistenceCheckConditionBean[] conditions,
      String whatCannotBeDoneMessageId, String referingRecordDataLabel,
      String recordSpecifyingFieldName) throws BizLogicAppException {
    List<T> list = new ArrayList<>();

    if (optional.isPresent()) {
      list.add(optional.get());
    }

    internalChildExistenceCheck(list, referingEntityMessageId, conditions,
        whatCannotBeDoneMessageId, referingRecordDataLabel, recordSpecifyingFieldName);
  }

  /**
   * Contains attributes for child existence check condition.
   */
  public static record ChildExistenceCheckConditionBean(String itemPropertyPath,
      Object conditionValue, String messageIdDesignatingWhatCannotBeDone) {
  }
}
