  package com.chandan.payments.service;

  import static org.junit.jupiter.api.Assertions.*;
  import static org.mockito.Mockito.*;

  import com.chandan.payments.service.impl.statusprocessors.*;
  import com.chandan.payments.service.interfaces.TransactionStatusProcessor;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.context.ApplicationContext;

  /**
   * Unit tests for {@link PaymentStatusFactory}.
   *
   * The factory maps a numeric statusId (1‑6) to a concrete
   * {@link TransactionStatusProcessor} implementation fetched from the Spring
   * {@link ApplicationContext}.
   */
  @ExtendWith(MockitoExtension.class)
  class PaymentStatusFactoryTest {

      @Mock
      private ApplicationContext ctx;   // mocked Spring context

      private PaymentStatusFactory factory;

      @BeforeEach
      void setUp() {
          // inject the mocked ApplicationContext into the factory
          factory = new PaymentStatusFactory(ctx);
      }

      @Test
      void shouldReturnCreatedProcessor() {
          CreatedStatusProcessor createdMock = mock(CreatedStatusProcessor.class);
          when(ctx.getBean(CreatedStatusProcessor.class)).thenReturn(createdMock);

          TransactionStatusProcessor proc = factory.getStatusProcessor(1);
          assertTrue(proc instanceof CreatedStatusProcessor);
      }

      @Test
      void shouldReturnInitiatedProcessor() {
          InitiatedStatusProcessor initiatedMock = mock(InitiatedStatusProcessor.class);
          when(ctx.getBean(InitiatedStatusProcessor.class)).thenReturn(initiatedMock);

          TransactionStatusProcessor proc = factory.getStatusProcessor(2);
          assertTrue(proc instanceof InitiatedStatusProcessor);
      }

      @Test
      void shouldReturnApprovedProcessor() {
          ApprovedStatusProcessor approvedMock = mock(ApprovedStatusProcessor.class);
          when(ctx.getBean(ApprovedStatusProcessor.class)).thenReturn(approvedMock);

          TransactionStatusProcessor proc = factory.getStatusProcessor(4);
          assertTrue(proc instanceof ApprovedStatusProcessor);
      }

      @Test
      void shouldReturnPendingProcessor() {
          PendingStatusProcessor pendingMock = mock(PendingStatusProcessor.class);
          when(ctx.getBean(PendingStatusProcessor.class)).thenReturn(pendingMock);

          TransactionStatusProcessor proc = factory.getStatusProcessor(3);
          assertTrue(proc instanceof PendingStatusProcessor);
      }

      @Test
      void shouldReturnFailedProcessor() {
          FailedStatusProcessor failedMock = mock(FailedStatusProcessor.class);
          when(ctx.getBean(FailedStatusProcessor.class)).thenReturn(failedMock);

          TransactionStatusProcessor proc = factory.getStatusProcessor(6);
          assertTrue(proc instanceof FailedStatusProcessor);
      }

      @Test
      void shouldReturnSuccessProcessor() {
          SuccessStatusProcessor successMock = mock(SuccessStatusProcessor.class);
          when(ctx.getBean(SuccessStatusProcessor.class)).thenReturn(successMock);

          TransactionStatusProcessor proc = factory.getStatusProcessor(5);
          assertTrue(proc instanceof SuccessStatusProcessor);
      }

      @Test
      void shouldReturnNullForUnknownStatus() {
          // IDs outside 1‑6 should give null per the factory implementation
          assertNull(factory.getStatusProcessor(0));
          assertNull(factory.getStatusProcessor(99));
      }
  }