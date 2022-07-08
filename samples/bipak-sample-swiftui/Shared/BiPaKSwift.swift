//
// BiPaK library sample swift code
// Copyright 2022 Nicolas Haan.
//
import SwiftUI
import SampleCommon


struct PagingView<Content: View>: View {
    var content: () -> Content
    
    init(eventEmitter: PagingEventEmitter, id: Int, @ViewBuilder content: @escaping () -> Content) {
        eventEmitter.onGetItem(index: Int32(id))
        self.content = content
    }
    
    var body: some View {
        content()
    }
}

public func empyPagingData<T>() -> PagingData<T> {
    return PagingData(
        list: [],state: PagingDataLoadState.NotLoading(), totalCount: 0)
}
