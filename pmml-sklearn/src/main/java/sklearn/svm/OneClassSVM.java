/*
 * Copyright (c) 2016 Villu Ruusmann
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
package sklearn.svm;

import org.dmg.pmml.support_vector_machine.SupportVectorMachineModel;
import org.jpmml.converter.Schema;
import sklearn.HasDecisionFunctionField;

public class OneClassSVM extends LibSVMRegressor implements HasDecisionFunctionField {

	public OneClassSVM(String module, String name){
		super(module, name);
	}

	@Override
	public boolean isSupervised(){
		return false;
	}

	@Override
	public SupportVectorMachineModel encodeModel(Schema schema){
		SupportVectorMachineModel supportVectorMachineModel = super.encodeModel(schema)
			.setOutput(OneClassSVMUtil.createPredictedOutput(this));

		return supportVectorMachineModel;
	}
}