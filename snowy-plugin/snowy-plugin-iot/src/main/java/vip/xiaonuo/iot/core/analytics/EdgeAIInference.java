package vip.xiaonuo.iot.core.analytics;

import ai.onnxruntime.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 边缘AI推理引擎
 * 支持ONNX模型本地推理
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Slf4j
@Component
public class EdgeAIInference {
    
    /** ONNX运行时环境 */
    private OrtEnvironment env;
    
    /** 模型会话缓存 <modelName, OrtSession> */
    private final Map<String, OrtSession> sessionCache = new ConcurrentHashMap<>();
    
    /** 模型信息缓存 */
    private final Map<String, ModelInfo> modelInfoCache = new ConcurrentHashMap<>();
    
    /**
     * 初始化ONNX运行时环境
     */
    private synchronized void initEnvironment() {
        if (env == null) {
            env = OrtEnvironment.getEnvironment();
            log.info("ONNX Runtime环境初始化完成，版本: {}", env.getVersion());
        }
    }
    
    /**
     * 加载ONNX模型
     * 
     * @param modelName 模型名称（用于缓存标识）
     * @param modelPath 模型文件路径
     * @throws OrtException ONNX运行时异常
     */
    public void loadModel(String modelName, String modelPath) throws OrtException {
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            throw new IllegalArgumentException("模型文件不存在: " + modelPath);
        }
        
        initEnvironment();
        
        // 配置会话选项
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        options.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors() / 2);
        
        // 创建会话
        OrtSession session = env.createSession(modelPath, options);
        
        // 缓存会话
        OrtSession oldSession = sessionCache.put(modelName, session);
        if (oldSession != null) {
            oldSession.close();
        }
        
        // 记录模型信息
        ModelInfo modelInfo = extractModelInfo(session);
        modelInfoCache.put(modelName, modelInfo);
        
        log.info("ONNX模型加载成功: name={}, path={}, inputs={}, outputs={}", 
            modelName, modelPath, modelInfo.getInputNames(), modelInfo.getOutputNames());
    }
    
    /**
     * 执行推理
     * 
     * @param modelName 模型名称
     * @param inputData 输入数据
     * @return 推理结果
     * @throws OrtException ONNX运行时异常
     */
    public float[] inference(String modelName, float[] inputData) throws OrtException {
        OrtSession session = sessionCache.get(modelName);
        if (session == null) {
            throw new IllegalStateException("模型未加载: " + modelName);
        }
        
        ModelInfo modelInfo = modelInfoCache.get(modelName);
        String inputName = modelInfo.getInputNames().isEmpty() ? "input" : modelInfo.getInputNames().get(0);
        
        // 创建输入Tensor
        long[] shape = {1, inputData.length};
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape)) {
            
            // 执行推理
            Map<String, OnnxTensor> inputs = Map.of(inputName, inputTensor);
            try (OrtSession.Result result = session.run(inputs)) {
                
                // 获取输出
                Object outputValue = result.get(0).getValue();
                
                if (outputValue instanceof float[][] output2D) {
                    return output2D[0];
                } else if (outputValue instanceof float[] output1D) {
                    return output1D;
                } else {
                    throw new IllegalStateException("不支持的输出类型: " + outputValue.getClass());
                }
            }
        }
    }
    
    /**
     * 执行批量推理
     * 
     * @param modelName 模型名称
     * @param batchData 批量输入数据 [batch_size, features]
     * @return 批量推理结果
     * @throws OrtException ONNX运行时异常
     */
    public float[][] batchInference(String modelName, float[][] batchData) throws OrtException {
        OrtSession session = sessionCache.get(modelName);
        if (session == null) {
            throw new IllegalStateException("模型未加载: " + modelName);
        }
        
        ModelInfo modelInfo = modelInfoCache.get(modelName);
        String inputName = modelInfo.getInputNames().isEmpty() ? "input" : modelInfo.getInputNames().get(0);
        
        int batchSize = batchData.length;
        int featureSize = batchData[0].length;
        
        // 将2D数组展平为1D
        float[] flatData = new float[batchSize * featureSize];
        for (int i = 0; i < batchSize; i++) {
            System.arraycopy(batchData[i], 0, flatData, i * featureSize, featureSize);
        }
        
        long[] shape = {batchSize, featureSize};
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(flatData), shape)) {
            
            Map<String, OnnxTensor> inputs = Map.of(inputName, inputTensor);
            try (OrtSession.Result result = session.run(inputs)) {
                
                Object outputValue = result.get(0).getValue();
                
                if (outputValue instanceof float[][] output2D) {
                    return output2D;
                } else {
                    throw new IllegalStateException("批量推理需要2D输出");
                }
            }
        }
    }
    
    /**
     * 设备故障预测
     * 
     * @param modelName 预测模型名称
     * @param deviceMetrics 设备指标数据
     * @return 故障预测结果
     */
    public FailurePrediction predictFailure(String modelName, Map<String, Double> deviceMetrics) {
        try {
            // 将设备指标转换为特征向量
            float[] features = extractFeatures(deviceMetrics);
            
            // 执行推理
            float[] prediction = inference(modelName, features);
            
            // 解析预测结果
            double probability = prediction.length > 0 ? prediction[0] : 0.0;
            String riskLevel = determineRiskLevel(probability);
            
            return new FailurePrediction(
                true,
                probability,
                riskLevel,
                System.currentTimeMillis(),
                null
            );
            
        } catch (Exception e) {
            log.error("故障预测失败", e);
            return new FailurePrediction(
                false,
                0.0,
                "UNKNOWN",
                System.currentTimeMillis(),
                e.getMessage()
            );
        }
    }
    
    /**
     * 提取特征向量
     */
    private float[] extractFeatures(Map<String, Double> metrics) {
        // 默认特征列表（可配置）
        String[] featureKeys = {"temperature", "vibration", "current", "voltage", "pressure", "humidity"};
        
        float[] features = new float[featureKeys.length];
        for (int i = 0; i < featureKeys.length; i++) {
            features[i] = metrics.getOrDefault(featureKeys[i], 0.0).floatValue();
        }
        
        return features;
    }
    
    /**
     * 确定风险等级
     */
    private String determineRiskLevel(double probability) {
        if (probability >= 0.8) {
            return "CRITICAL";
        } else if (probability >= 0.6) {
            return "HIGH";
        } else if (probability >= 0.4) {
            return "MEDIUM";
        } else if (probability >= 0.2) {
            return "LOW";
        } else {
            return "NORMAL";
        }
    }
    
    /**
     * 提取模型信息
     */
    private ModelInfo extractModelInfo(OrtSession session) throws OrtException {
        Map<String, NodeInfo> inputInfo = session.getInputInfo();
        Map<String, NodeInfo> outputInfo = session.getOutputInfo();
        
        return new ModelInfo(
            inputInfo.keySet().stream().toList(),
            outputInfo.keySet().stream().toList()
        );
    }
    
    /**
     * 卸载模型
     */
    public void unloadModel(String modelName) {
        OrtSession session = sessionCache.remove(modelName);
        if (session != null) {
            try {
                session.close();
                log.info("模型已卸载: {}", modelName);
            } catch (OrtException e) {
                log.warn("关闭模型会话失败: {}", modelName, e);
            }
        }
        modelInfoCache.remove(modelName);
    }
    
    /**
     * 检查模型是否已加载
     */
    public boolean isModelLoaded(String modelName) {
        return sessionCache.containsKey(modelName);
    }
    
    /**
     * 获取已加载的模型列表
     */
    public java.util.Set<String> getLoadedModels() {
        return sessionCache.keySet();
    }
    
    /**
     * 获取模型信息
     */
    public ModelInfo getModelInfo(String modelName) {
        return modelInfoCache.get(modelName);
    }
    
    @PreDestroy
    public void shutdown() {
        sessionCache.forEach((name, session) -> {
            try {
                session.close();
            } catch (OrtException e) {
                log.warn("关闭模型会话失败: {}", name, e);
            }
        });
        sessionCache.clear();
        modelInfoCache.clear();
        
        log.info("边缘AI推理引擎已关闭");
    }
    
    /**
     * 模型信息
     */
    @Data
    @AllArgsConstructor
    public static class ModelInfo {
        /** 输入名称列表 */
        private java.util.List<String> inputNames;
        /** 输出名称列表 */
        private java.util.List<String> outputNames;
    }
    
    /**
     * 故障预测结果
     */
    @Data
    @AllArgsConstructor
    public static class FailurePrediction {
        /** 预测是否成功 */
        private boolean success;
        /** 故障概率 (0-1) */
        private double probability;
        /** 风险等级 */
        private String riskLevel;
        /** 预测时间 */
        private long timestamp;
        /** 错误信息（如果预测失败） */
        private String errorMessage;
    }
}
