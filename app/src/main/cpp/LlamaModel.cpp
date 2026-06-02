//
// Native model wrapper for NEO Local LM.
//

#include <jni.h>
#include <string>

#include "LlamaCpp.h"
#include "common.h"
#include "chat.h"

#include "console.h"
#include "ggml-backend.h"
#include "log.h"

#include <cassert>
#include <cinttypes>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <ctime>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <string>
#include <utility>
#include <vector>
#include <mutex>
#include <algorithm>

#include <csignal>
#include <unistd.h>
#include <android/log.h>
#include <fcntl.h>

namespace {
std::vector<ggml_backend_dev_t> preferred_offload_devices() {
    std::vector<ggml_backend_dev_t> htp_devices;
    std::vector<ggml_backend_dev_t> gpu_devices;

    for (size_t i = 0; i < ggml_backend_dev_count(); ++i) {
        ggml_backend_dev_t dev = ggml_backend_dev_get(i);
        const char *name = ggml_backend_dev_name(dev);
        const enum ggml_backend_dev_type type = ggml_backend_dev_type(dev);

        if (name != nullptr && std::strncmp(name, "HTP", 3) == 0) {
            htp_devices.push_back(dev);
        } else if (type == GGML_BACKEND_DEVICE_TYPE_GPU || type == GGML_BACKEND_DEVICE_TYPE_IGPU) {
            gpu_devices.push_back(dev);
        }
    }

    std::sort(htp_devices.begin(), htp_devices.end(), [](ggml_backend_dev_t a, ggml_backend_dev_t b) {
        return std::strcmp(ggml_backend_dev_name(a), ggml_backend_dev_name(b)) < 0;
    });

    if (!htp_devices.empty()) {
        LOG_INF("Using Hexagon HTP devices for local model offload:");
        for (auto *dev : htp_devices) {
            LOG_INF(" %s", ggml_backend_dev_name(dev));
        }
        LOG_INF("\n");
        htp_devices.push_back(nullptr);
        return htp_devices;
    }

    if (!gpu_devices.empty()) {
        LOG_INF("Using GPU devices for local model offload:");
        for (auto *dev : gpu_devices) {
            LOG_INF(" %s", ggml_backend_dev_name(dev));
        }
        LOG_INF("\n");
        gpu_devices.push_back(nullptr);
        return gpu_devices;
    }

    return {};
}
}

void LlamaModel::loadModel(const std::string &modelPath,
                           int32_t n_gpu_layers,
                           llama_progress_callback progress_callback,
                           void * progress_callback_user_data) {

    // initialize the model
    llama_model_params model_params = llama_model_default_params();
    std::vector<ggml_backend_dev_t> devices = preferred_offload_devices();
    if (!devices.empty()) {
        model_params.devices = devices.data();
    }
    model_params.n_gpu_layers = n_gpu_layers;
    model_params.split_mode = LLAMA_SPLIT_MODE_LAYER;
    model_params.use_mmap = false;
    model_params.use_mlock = true;
    model_params.progress_callback = progress_callback;
    model_params.progress_callback_user_data = progress_callback_user_data;
    model = llama_model_load_from_file(modelPath.c_str(), model_params);
    if (model == nullptr) {
        LOG_ERR("%s: failed to load model '%s'\n", __func__, modelPath.c_str());
        return;
    }
    chat_tmpls = common_chat_templates_init(model, "");
}

LlamaGenerationSession* LlamaModel::createGenerationSession(const SamplerParams &params) {
    if (model == nullptr) {
        return nullptr;
    }
    auto *session = new LlamaGenerationSession();
    session->init(model, chat_tmpls.get(), params);
    return session;
}

int LlamaModel::getContextTrainSize() {
    if (model == nullptr) {
        return 0;
    }
    return llama_model_n_ctx_train(model);
}

uint64_t LlamaModel::getModelSize() {
    if (this->model == nullptr) {
        return 0;
    }
    return llama_model_size(this->model);
}

bool LlamaModel::supportsThinking() {
    if (!chat_tmpls) {
        return false;
    }
    return common_chat_templates_support_enable_thinking(chat_tmpls.get());
}

std::string LlamaModel::getModelReport() {
    if (model == nullptr) {
        return "";
    }

    char desc[256];
    llama_model_desc(model, desc, sizeof(desc));

    uint64_t n_params = llama_model_n_params(model);
    int n_ctx_train = llama_model_n_ctx_train(model);

    std::ostringstream report;
    report << "Model\n";
    report << "  Architecture: " << desc << "\n";

    if (n_params >= 1000000000ULL) {
        report << "  Parameters: " << std::fixed << std::setprecision(2)
               << (n_params / 1e9) << "B\n";
    } else {
        report << "  Parameters: " << std::fixed << std::setprecision(0)
               << (n_params / 1e6) << "M\n";
    }

    report << "  Training context: " << n_ctx_train << "\n";

    return report.str();
}

void LlamaModel::unloadModel() {
    chat_tmpls.reset();
    if (model != nullptr) {
        llama_model_free(model);
        model = nullptr;
    }
}
