/*
 * Copyright (c) 2020 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package sklearn;

import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.DataType;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.python.PythonObject;
import sklearn2pmml.SkLearn2PMMLFields;

abstract
public class Step extends PythonObject implements HasNumberOfFeatures, HasType {

	public Step(String module, String name){
		super(module, name);
	}

	public String createFieldName(DataType dataType, Object... args){
		return createFieldName((dataType.name()).toLowerCase(), args);
	}

	public String createFieldName(String function, Object... args){
		return createFieldName(function, Arrays.asList(args));
	}

	public String createFieldName(String function, List<?> args){
		String pmmlName = getPMMLName();

		if(pmmlName != null){
			return pmmlName;
		}

		return FieldNameUtil.create(function, args);
	}

	public String getPMMLName(){
		return getOptionalString(SkLearn2PMMLFields.PMML_NAME);
	}

	public Step setPMMLName(String name){
		put(SkLearn2PMMLFields.PMML_NAME, name);

		return this;
	}
}