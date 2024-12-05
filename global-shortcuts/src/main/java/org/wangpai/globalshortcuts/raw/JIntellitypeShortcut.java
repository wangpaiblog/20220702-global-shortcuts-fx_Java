package org.wangpai.globalshortcuts.raw;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @since 2022-9-30
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class JIntellitypeShortcut {
    private int modifier;
    private int keycode;

    /**
     * @since 2022-9-30
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof JIntellitypeShortcut other)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (this.modifier == other.modifier && this.keycode == other.keycode) {
            return true;
        }
        return false;
    }

    /**
     * @since 2022-9-30
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.modifier, this.keycode);
    }
}
