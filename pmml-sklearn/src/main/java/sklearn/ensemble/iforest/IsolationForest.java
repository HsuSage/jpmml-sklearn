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
package sklearn.ensemble.iforest;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.google.common.primitives.Ints;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.mining.Segmentation.MultipleModelMethod;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.PredicateManager;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ScoreDistributionManager;
import org.jpmml.converter.Transformation;
import org.jpmml.converter.ValueUtil;
import org.jpmml.converter.mining.MiningModelUtil;
import org.jpmml.converter.transformations.AbstractTransformation;
import org.jpmml.converter.transformations.OutlierTransformation;
import org.jpmml.model.visitors.AbstractVisitor;
import org.jpmml.python.ClassDictUtil;
import org.jpmml.python.HasArray;
import sklearn.HasDecisionFunctionField;
import sklearn.Regressor;
import sklearn.SkLearnOutlierTransformation;
import sklearn.SkLearnUtil;
import sklearn.ensemble.EnsembleRegressor;
import sklearn.ensemble.EnsembleUtil;
import sklearn.tree.HasTreeOptions;
import sklearn.tree.Tree;
import sklearn.tree.TreeRegressor;
import sklearn.tree.TreeUtil;

public class IsolationForest extends EnsembleRegressor implements HasDecisionFunctionField, HasTreeOptions {

	public IsolationForest(String module, String name){
		super(module, name);
	}

	@Override
	public boolean isSupervised(){
		return false;
	}

	@Override
	public MiningModel encodeModel(Schema schema){
		String sklearnVersion = getSkLearnVersion();
		List<? extends Regressor> estimators = getEstimators();
		List<List<Integer>> estimatorsFeatures = getEstimatorsFeatures();

		// See https://github.com/scikit-learn/scikit-learn/issues/8549
		boolean corrected = (sklearnVersion != null && SkLearnUtil.compareVersion(sklearnVersion, "0.19") >= 0);

		// See https://github.com/scikit-learn/scikit-learn/issues/11839
		boolean nodeSampleCorrected = (sklearnVersion != null && SkLearnUtil.compareVersion(sklearnVersion, "0.21") >= 0);

		Boolean numeric = (Boolean)getOption(HasTreeOptions.OPTION_NUMERIC, Boolean.TRUE);

		PredicateManager predicateManager = new PredicateManager();
		ScoreDistributionManager scoreDistributionManager = new ScoreDistributionManager();

		Schema segmentSchema = schema.toAnonymousSchema();

		List<TreeModel> treeModels = new ArrayList<>();

		for(int i = 0; i < estimators.size(); i++){
			Regressor estimator = estimators.get(i);
			List<Integer> estimatorFeatures = estimatorsFeatures.get(i);

			Schema estimatorSchema = segmentSchema.toSubSchema(Ints.toArray(estimatorFeatures));

			TreeRegressor treeRegressor = (TreeRegressor)estimator;

			Tree tree = treeRegressor.getTree();

			TreeModel treeModel = TreeUtil.encodeTreeModel(treeRegressor, MiningFunction.REGRESSION, numeric, predicateManager, scoreDistributionManager, estimatorSchema);

			Visitor visitor = new AbstractVisitor(){

				private int[] nodeSamples = tree.getNodeSamples();


				@Override
				public VisitorAction visit(Node node){

					if(node.hasScore()){
						double nodeDepth = 0d;

						Deque<PMMLObject> parents = getParents();
						for(PMMLObject parent : parents){

							if(!(parent instanceof Node)){
								break;
							}

							nodeDepth++;
						}

						double nodeSample = this.nodeSamples[ValueUtil.asInt((Number)node.getId())];

						double averagePathLength = (corrected ? correctedAveragePathLength(nodeSample, nodeSampleCorrected) : averagePathLength(nodeSample));

						node.setScore(nodeDepth + averagePathLength);
					}

					return super.visit(node);
				}
			};
			visitor.applyTo(treeModel);

			ClassDictUtil.clearContent(tree);

			treeModels.add(treeModel);
		}

		// "rawAnomalyScore / averagePathLength(maxSamples)"
		Transformation normalizedAnomalyScore = new AbstractTransformation(){

			@Override
			public String getName(String name){
				return "normalizedAnomalyScore";
			}

			@Override
			public Expression createExpression(FieldRef fieldRef){
				double maxSamples = getMaxSamples();

				double averagePathLength = (corrected ? correctedAveragePathLength(maxSamples, nodeSampleCorrected) : averagePathLength(maxSamples));

				return PMMLUtil.createApply(PMMLFunctions.DIVIDE, fieldRef, PMMLUtil.createConstant(averagePathLength));
			}
		};

		// "0.5 - 2 ^ (-1 * normalizedAnomalyScore)"
		Transformation decisionFunction = new AbstractTransformation(){

			@Override
			public String getName(String name){
				return getDecisionFunctionField();
			}

			@Override
			public boolean isFinalResult(){
				return true;
			}

			@Override
			public Expression createExpression(FieldRef fieldRef){
				Number offset = getOffset();

				return PMMLUtil.createApply(PMMLFunctions.SUBTRACT, PMMLUtil.createConstant(-offset.doubleValue()), PMMLUtil.createApply(PMMLFunctions.POW, PMMLUtil.createConstant(2d), PMMLUtil.createApply(PMMLFunctions.MULTIPLY, PMMLUtil.createConstant(-1d), fieldRef)));
			}
		};

		Transformation outlier = new OutlierTransformation(){

			@Override
			public String getName(String name){
				return createFieldName("outlier");
			}

			@Override
			public Expression createExpression(FieldRef fieldRef){
				String behaviour = getBehaviour();

				Number threshold;

				// SkLearn 0.19 or SkLearn 0.24+
				if(behaviour == null){
					threshold = getThreshold();
				} else

				// SkLearn 0.20 through 0.23
				{
					if(("old").equals(behaviour)){
						threshold = getThreshold();
					} else

					if(("new").equals(behaviour) || ("deprecated").equals(behaviour)){
						threshold = 0d;
					} else

					{
						throw new IllegalArgumentException(behaviour);
					}
				}

				return PMMLUtil.createApply(PMMLFunctions.LESSOREQUAL, fieldRef, PMMLUtil.createConstant(threshold));
			}
		};

		Transformation sklearnOutlier = new SkLearnOutlierTransformation();

		Output output = ModelUtil.createPredictedOutput("rawAnomalyScore", OpType.CONTINUOUS, DataType.DOUBLE, normalizedAnomalyScore, decisionFunction, outlier, sklearnOutlier);

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(schema.getLabel()))
			.setSegmentation(MiningModelUtil.createSegmentation(MultipleModelMethod.AVERAGE, Segmentation.MissingPredictionTreatment.RETURN_MISSING, treeModels))
			.setOutput(output);

		return TreeUtil.transform(this, miningModel);
	}

	public List<List<Integer>> getEstimatorsFeatures(){
		return EnsembleUtil.transformEstimatorsFeatures(getList("estimators_features_", HasArray.class));
	}

	public String getBehaviour(){
		return getOptionalString("behaviour");
	}

	public int getMaxSamples(){
		return getInteger("max_samples_");
	}

	public Number getOffset(){

		if(!containsKey("offset_")){
			return 0.5d;
		}

		return getNumber("offset_");
	}

	public Number getThreshold(){

		// SkLearn 0.19
		if(containsKey("threshold_")){
			return getNumber("threshold_");
		} else

		// SkLearn 0.20+
		if(containsKey("_threshold_")){
			return getNumber("_threshold_");
		}

		// SkLearn 0.24+
		return 0d;
	}

	static
	private double averagePathLength(double n){

		if(n <= 1d){
			return 1d;
		}

		return 2d * (Math.log(n) + 0.5772156649) - 2d * ((n - 1d) / n);
	}

	static
	private double correctedAveragePathLength(double n, boolean nodeSampleCorrected){

		if(nodeSampleCorrected){

			if(n <= 1d){
				return 0d;
			} else

			if(n <= 2d){
				return 1d;
			}
		} else

		{
			if(n <= 1d){
				return 1d;
			}
		}

		return 2d * (Math.log(n - 1d) + 0.577215664901532860606512090082402431d) - 2d * ((n - 1d) / n);
	}
}