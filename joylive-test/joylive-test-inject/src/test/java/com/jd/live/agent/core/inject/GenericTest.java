/*
 * Copyright © ${year} ${owner} (${email})
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
package com.jd.live.agent.core.inject;

import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.core.util.type.FieldList;
import com.jd.live.agent.core.util.type.generic.Generic;
import com.jd.live.agent.core.util.type.generic.GenericVariable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GenericTest {
    @Test
    public void testGeneric() throws NoSuchMethodException {

        BookRoom room = new BookRoom();

        ClassDesc classDesc = ClassUtils.describe(room.getClass());
        FieldList fieldList = classDesc.getFieldList();
        FieldDesc booksField = fieldList.getField("books");
        FieldDesc itemsField = fieldList.getField("items");
        FieldDesc personsField = fieldList.getField("persons");
        Generic generic = booksField.getGeneric();
        GenericVariable variable = generic.getVariable("M");
        Assertions.assertNotNull(variable);
        Assertions.assertEquals(Book.class, variable.getType());
        generic = itemsField.getGeneric();
        variable = generic.getVariable("T");
        Assertions.assertNotNull(variable);
        Assertions.assertEquals(Desktop.class, variable.getType());
        generic = personsField.getGeneric();
        Assertions.assertEquals(int.class, generic.getErasure().getComponentType());
    }


    protected static class Room<T> {
        private List<T> items;
    }

    protected static class DeskRoom<M> extends Room<Desktop> {

        private M[] books;

    }

    protected static class BookRoom extends DeskRoom<Book> {

        private int[] persons;
    }

    protected static class Desktop {

        private int size;
    }

    protected static class Book {
        private double price;
    }
}
