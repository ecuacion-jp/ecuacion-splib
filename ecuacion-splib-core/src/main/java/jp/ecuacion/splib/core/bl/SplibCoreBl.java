package jp.ecuacion.splib.core.bl;

import java.util.Arrays;
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.item.EclibItemContainer;
import jp.ecuacion.lib.core.util.PropertyFileUtil.Arg;
import jp.ecuacion.lib.core.util.StringUtil;

/**
 * Supplies utilities.
 */
public class SplibCoreBl {

  /**
   * Throws an exception when duplicated. 
   * It only has a function to throw exception, not one to check the occuring of duplication.
   */
  public static void throwExceptionWhenDuplicated(boolean isDuplicated, String[] itemPropertyPaths,
      EclibItemContainer container) throws BizLogicAppException {
    List<String> itemNameKeyList = Arrays.asList(itemPropertyPaths).stream()
        .map(path -> container.getItem(path).getItemNameKey()).toList();

    throwExceptionWhenDuplicated(isDuplicated, itemPropertyPaths,
        itemNameKeyList.toArray(new String[itemNameKeyList.size()]));
  }

  /**
   * Throws an exception when duplicated. 
   * It only has a function to throw exception, not one to check the occuring of duplication.
   */
  public static void throwExceptionWhenDuplicated(boolean isDuplicated, String[] itemPropertyPaths,
      String[] itemNameKeyList) throws BizLogicAppException {
    if (isDuplicated) {
      String msgId = "jp.ecuacion.splib.core.service.SplibEditJpaService.message."
          + (itemPropertyPaths.length == 1 ? "duplicated" : "combinationDuplicated");
      String str = StringUtil.getSeparatedValuesString(itemNameKeyList,
          "${+messages:jp.ecuacion.lib.core.common.itemName.separator}",
          "${+messages:jp.ecuacion.lib.core.common.itemName.prependParenthesis}${+item_names:",
          "}${+messages:jp.ecuacion.lib.core.common.itemName.appendParenthesis}", false);
      throw new BizLogicAppException(itemPropertyPaths, msgId,
          new Arg[] {Arg.formattedString(str)});
    }
  }
}
