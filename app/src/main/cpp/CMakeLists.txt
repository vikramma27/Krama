cmake_minimum_required(VERSION 3.22.1)

project("krama_image_processor")

add_library(
        krama_image_processor
        SHARED
        native_image_compressor.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        krama_image_processor
        ${log-lib}
)
